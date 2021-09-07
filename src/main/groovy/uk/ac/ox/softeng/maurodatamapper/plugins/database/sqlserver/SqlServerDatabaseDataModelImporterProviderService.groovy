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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.RemoteDatabaseDataModelImporterProviderService
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
    String columnRangeDistributionQueryString(String schemaName, String tableName, String columnName, DataType dataType, AbstractIntervalHelper intervalHelper) {
        List<String> selects = intervalHelper.intervals.collect {
            "SELECT '${it.key}' AS interval_label, ${formatDataType(dataType, it.value.aValue)} AS interval_start, ${formatDataType(dataType, it.value.bValue)} AS interval_end"
        }

        rangeDistributionQueryString(schemaName, tableName, columnName, selects)
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
    private String rangeDistributionQueryString(String schemaName, String tableName, String columnName, List<String> selects) {
        String intervals = selects.join(" UNION ")

        String sql = "WITH #interval AS (${intervals})" +
                """
        SELECT interval_label, COUNT([${columnName}]) AS interval_count
        FROM #interval
        LEFT JOIN
        ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)} 
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
}
