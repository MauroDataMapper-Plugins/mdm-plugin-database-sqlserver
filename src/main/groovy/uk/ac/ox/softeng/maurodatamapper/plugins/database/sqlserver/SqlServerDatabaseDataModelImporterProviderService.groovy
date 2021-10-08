/*
 * Copyright 2020-2021 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.RemoteDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.SamplingStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.summarymetadata.AbstractIntervalHelper
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

import java.sql.Connection
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter

@Slf4j
// @CompileStatic
class SqlServerDatabaseDataModelImporterProviderService
    extends AbstractDatabaseDataModelImporterProviderService<SqlServerDatabaseDataModelImporterProviderServiceParameters>
    implements RemoteDatabaseDataModelImporterProviderService {

    @Override
    SamplingStrategy getSamplingStrategy(SqlServerDatabaseDataModelImporterProviderServiceParameters parameters) {
        new SamplingStrategy(parameters.sampleThreshold ?: DEFAULT_SAMPLE_THRESHOLD, parameters.samplePercent ?: DEFAULT_SAMPLE_PERCENTAGE)
    }

    @Override
    String getDisplayName() {
        'MS SQL Server Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getIndexInformationQueryString() {
        '''
        SELECT OBJECT_NAME(i.object_id)           AS table_name,
               i.name                             AS index_name,
               i.is_unique                        AS unique_index,
               i.is_primary_key                   AS primary_index,
               CAST(
                 (CASE i.type_desc
                    WHEN 'CLUSTERED' THEN 1
                    ELSE 0 END)
                 AS BIT)                          AS is_clustered,
               CAST(
                 (SUBSTRING(
                   (SELECT ', ' + COL_NAME(ic2.object_id, ic2.column_id) AS [text()]
                    FROM sys.index_columns ic2
                    WHERE i.object_id = ic2.object_id
                      AND i.index_id = ic2.index_id
                    ORDER BY ic2.index_column_id
                       FOR XML PATH ('')), 2, 1000)) AS VARCHAR(1000)) AS column_names
        FROM sys.indexes AS i
               INNER JOIN sys.index_columns AS ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
               LEFT JOIN  sys.objects as o ON i.object_id = o.object_id
        WHERE OBJECT_SCHEMA_NAME(i.object_id) = ?
        GROUP BY i.object_id, i.index_id, i.name, i.is_unique, i.is_primary_key, i.type_desc;
        '''.stripIndent()
    }

    @Override
    String getForeignKeyInformationQueryString() {
        '''
        SELECT
          f.name                                                     AS constraint_name,
          OBJECT_NAME(f.parent_object_id)                            AS table_name,
          COL_NAME(fc.parent_object_id, fc.parent_column_id)         AS column_name,
          OBJECT_NAME(f.referenced_object_id)                        AS reference_table_name,
          COL_NAME(fc.referenced_object_id, fc.referenced_column_id) AS reference_column_name
        FROM sys.foreign_keys AS f
          INNER JOIN sys.foreign_key_columns AS fc
            ON f.OBJECT_ID = fc.constraint_object_id
        INNER JOIN sys.schemas AS s ON f.schema_id = s.schema_id
        WHERE s.name = ?;
        '''.stripIndent()
    }

    @Override
    String getDatabaseStructureQueryString() {
        'SELECT * FROM INFORMATION_SCHEMA.COLUMNS;'
    }

    /**
     * SQL Server square bracket escaping of identifiers
     */
    @Override
    String escapeIdentifier(String identifier) {
        "[${identifier}]"
    }

    /**
     * Return a query that will select an approximate row count from the specified table.
     * See https://docs.microsoft.com/en-us/sql/relational-databases/system-dynamic-management-views/sys-dm-db-partition-stats-transact-sql?view=sql-server-ver15
     *
     * @param tableName
     * @param schemaName
     * @return
     */
    @Override
    List<String> approxCountQueryString(String tableName, String schemaName = null) {
        //use COUNT_BIG rather than COUNT
        String schemaIdentifier = schemaName ? "${escapeIdentifier(schemaName)}." : ""
        List<String> queryStrings = [
                "SELECT COUNT_BIG(*) AS approx_count FROM ${schemaIdentifier}${escapeIdentifier(tableName)}".toString()
                ]

        String query = """
        SELECT SUM(dm_db_partition_stats.row_count) AS approx_count
        FROM sys.dm_db_partition_stats
        WHERE object_id = OBJECT_ID('${tableName}')
        AND (index_id = 0 OR index_id = 1)
        """

        queryStrings.push(query.toString())
        queryStrings
    }

    @Override
    boolean isColumnPossibleEnumeration(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && (dataType.label == "char" || dataType.label == "varchar")
    }

    @Override
    boolean isColumnForDateSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ["date", "smalldatetime", "datetime", "datetime2"].contains(dataType.label)
    }

    @Override
    boolean isColumnForDecimalSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ["decimal", "numeric"].contains(dataType.label)
    }

    @Override
    boolean isColumnForIntegerSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ["tinyint", "smallint", "int", "bigint"].contains(dataType.label)
    }

    @Override
    String countDistinctColumnValuesQueryString(SamplingStrategy samplingStrategy, String columnName, String tableName, String schemaName = null) {
        String query = super.countDistinctColumnValuesQueryString(columnName, tableName, schemaName)

        if (samplingStrategy.useSampling()) {
            query = query + " TABLESAMPLE (${samplingStrategy.percentage} PERCENT)"
        }

        query
    }

    @Override
    String distinctColumnValuesQueryString(SamplingStrategy samplingStrategy, String columnName, String tableName, String schemaName = null) {
        String query = super.distinctColumnValuesQueryString(columnName, tableName, schemaName)

        if (samplingStrategy.useSampling()) {
            query = query + " TABLESAMPLE (${samplingStrategy.percentage} PERCENT)"
        }

        query
    }

    /**
     * Use the superclass method to construct a query string, and then append a TABLESAMPLE clause if necessary
     * @param samplingStrategy
     * @param columnName
     * @param tableName
     * @param schemaName
     * @return Query string, optionally with TABLESAMPLE clause appended
     */
    @Override
    String minMaxColumnValuesQueryString(SamplingStrategy samplingStrategy, String columnName, String tableName, String schemaName = null) {
        String query = super.minMaxColumnValuesQueryString(samplingStrategy, columnName, tableName, schemaName)

        if (samplingStrategy.useSampling()) {
            query = query + " TABLESAMPLE (${samplingStrategy.percentage} PERCENT)"
        }

        query
    }

    String columnRangeDistributionQueryString(DataType dataType,
                                              AbstractIntervalHelper intervalHelper,
                                              String columnName, String tableName, String schemaName) {
        SamplingStrategy samplingStrategy = new SamplingStrategy()
        columnRangeDistributionQueryString(samplingStrategy, dataType, intervalHelper, columnName, tableName, schemaName)
    }

    @Override
    String columnRangeDistributionQueryString(SamplingStrategy samplingStrategy, DataType dataType,
                                              AbstractIntervalHelper intervalHelper,
                                              String columnName, String tableName, String schemaName) {
        List<String> selects = intervalHelper.intervals.collect {
            "SELECT '${it.key}' AS interval_label, ${formatDataType(dataType, it.value.aValue)} AS interval_start, ${formatDataType(dataType, it.value.bValue)} AS interval_end"
        }

        rangeDistributionQueryString(samplingStrategy, selects, columnName, tableName, schemaName)
    }

    /**
     * Return a string which uses the SQL Server CONVERT function for Dates, otherwise string formatting
     *
     * @param dataType
     * @param value
     * @return Date formatted as ISO8601 (see
     * https://docs.microsoft.com/en-us/sql/t-sql/functions/cast-and-convert-transact-sql?view=sql-server-ver15)
     * or a string
     */
    String formatDataType(DataType dataType, Object value) {
        if (isColumnForDateSummary(dataType)){
            "CONVERT(DATETIME, '${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value)}', 126)"
        } else {
            "${value}"
        }
    }

    /**
     * Returns a String that looks, for example, like this:
     * WITH #interval AS (
     *   SELECT '0 - 100' AS interval_label, 0 AS interval_start, 100 AS interval_end
     *   UNION
     *   SELECT '100 - 200' AS interval_label, 100 AS interval_start, 200 AS interval_end
     * )
     * SELECT interval_label, COUNT([my_column]) AS interval_count
     * FROM #interval
     * LEFT JOIN
     * [my_table] ON [my_table].[my_column] >= #interval.interval_start AND [my_table].[my_column] < #interval.interval_end
     * GROUP BY interval_label, interval_start
     * ORDER BY interval_start ASC;
     *
     * @param tableName
     * @param columnName
     * @param selects
     * @return
     */
    private String rangeDistributionQueryString(SamplingStrategy samplingStrategy, List<String> selects, String columnName,
                                                String tableName, String schemaName) {
        String intervals = selects.join(" UNION ")

        String tableSample = ""
        if (samplingStrategy.useSampling()) {
            tableSample = " TABLESAMPLE (${samplingStrategy.percentage} PERCENT) "
        }

        String sql = "WITH #interval AS (${intervals})" +
                """
        SELECT interval_label, ${samplingStrategy.scaleFactor()} * COUNT([${columnName}]) AS interval_count
        FROM #interval
        LEFT JOIN
        ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)} 
        ${tableSample}
        ON ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)} >= #interval.interval_start 
        AND ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)} < #interval.interval_end
        GROUP BY interval_label, interval_start
        ORDER BY interval_start ASC;
        """

        sql.stripIndent()
    }

    @Override
    PreparedStatement prepareCoreStatement(Connection connection, SqlServerDatabaseDataModelImporterProviderServiceParameters parameters) {
        if (!parameters.schemaNames) return super.prepareCoreStatement(connection, parameters)
        final List<String> names = parameters.schemaNames.split(',') as List<String>
        final PreparedStatement statement = connection.prepareStatement(
            """SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA IN (${names.collect {'?'}.join(',')});""")
        names.eachWithIndex {String name, int i -> statement.setString(i + 1, name)}
        statement
    }

    List<DataModel> importAndUpdateDataModelsFromResults(User currentUser, String databaseName,
                                                         SqlServerDatabaseDataModelImporterProviderServiceParameters parameters, Folder folder,
                                                         String modelName, List<Map<String, Object>> results, Connection connection) {
        if (!parameters.importSchemasAsSeparateModels) {
            return super.importAndUpdateDataModelsFromResults(currentUser, databaseName, parameters, folder, modelName, results, connection)
        }

        log.debug 'Importing all schemas as separate DataModels'
        final Map<String, List<Map<String, Object>>> groupedSchemas = results.groupBy {row -> row[schemaNameColumnName] as String}

        groupedSchemas.collect {String schema, List<Map<String, Object>> schemaResults ->
            log.debug 'Importing database {} schema {}', databaseName, schema
            final DataModel dataModel = importDataModelFromResults(currentUser, folder, schema, parameters.databaseDialect, schemaResults, false)
            if (parameters.dataModelNameSuffix) dataModel.aliasesString = databaseName
            updateDataModelWithDatabaseSpecificInformation(dataModel, connection)
            dataModel
        }
    }

    /**
     * Use SQL Server fn_listextendedproperty to find extended properties
     * See https://docs.microsoft.com/en-us/sql/relational-databases/system-functions/sys-fn-listextendedproperty-transact-sql?view=sql-server-ver15
     * @param dataModel
     * @param connection
     */
    @Override
    void addMetadata(DataModel dataModel, Connection connection) {
        //Get extended properties for the database
        String databaseQuery = """
        SELECT name AS metadata_key, value as metadata_value
        FROM fn_listextendedproperty(NULL, NULL, NULL, NULL, NULL, NULL, NULL)
        """
        PreparedStatement preparedStatement = connection.prepareStatement(databaseQuery)
        List<Map<String, Object>> databaseMetadata = executeStatement(preparedStatement)

        databaseMetadata.each {Map<String, Object> row ->
            dataModel.addToMetadata(namespace, row.metadata_key as String, row.metadata_value as String, dataModel.createdBy)
        }

        //Get extended properties for the schema
        String schemaQuery = """
        SELECT name AS metadata_key, value as metadata_value, objname as schema_name
        FROM fn_listextendedproperty(NULL, 'schema', default, NULL, NULL, NULL, NULL)
        """
        preparedStatement = connection.prepareStatement(schemaQuery)
        List<Map<String, Object>> schemaMetadata = executeStatement(preparedStatement)

        schemaMetadata.each {Map<String, Object> row ->
            dataModel.childDataClasses.find{dc ->
                dc.label == row.schema_name
            }.addToMetadata(namespace, row.metadata_key as String, row.metadata_value as String, dataModel.createdBy)
        }

        dataModel.childDataClasses.each { DataClass schemaClass ->

            //Get extended properties for all tables in this schema
            String tableQuery = """
            SELECT name AS metadata_key, value as metadata_value, objname as table_name
            FROM fn_listextendedproperty(NULL, 'schema', '${schemaClass.label}', 'table', default, NULL, NULL)
            """
            preparedStatement = connection.prepareStatement(tableQuery)
            List<Map<String, Object>> tableMetadata = executeStatement(preparedStatement)

            tableMetadata.each {Map<String, Object> row ->
                schemaClass.dataClasses.find{dc ->
                    dc.label == row.table_name
                }.addToMetadata(namespace, row.metadata_key as String, row.metadata_value as String, dataModel.createdBy)
            }

            schemaClass.dataClasses.each { DataClass tableClass ->
                //Get extended properties for all columns for this table
                String columnQuery = """
                SELECT name AS metadata_key, value as metadata_value, objname as column_name
                FROM fn_listextendedproperty(NULL, 'schema', '${schemaClass.label}', 'table', '${tableClass.label}', 'column', default)
                """
                preparedStatement = connection.prepareStatement(columnQuery)
                List<Map<String, Object>> columnMetadata = executeStatement(preparedStatement)

                columnMetadata.each {Map<String, Object> row ->
                    tableClass.dataElements.find{de ->
                        de.label == row.column_name
                    }.addToMetadata(namespace, row.metadata_key as String, row.metadata_value as String, dataModel.createdBy)
                }
            }
        }
    }
}
