/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.RemoteDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.CalculationStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.SamplingStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.query.QueryStringProvider
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.calculation.SqlServerCalculationStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.calculation.SqlServerSamplingStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.parameters.SqlServerDatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.query.SqlServerQueryStringProvider
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.sql.Connection
import java.sql.PreparedStatement

@Slf4j
//@CompileStatic
class SqlServerDatabaseDataModelImporterProviderService
    extends AbstractDatabaseDataModelImporterProviderService<SqlServerDatabaseDataModelImporterProviderServiceParameters>
    implements RemoteDatabaseDataModelImporterProviderService {

    SqlServerDataTypeProviderService sqlServerDataTypeProviderService

    @Override
    String getDisplayName() {
        'MS SQL Server Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    Boolean handlesContentType(String contentType) {
        false
    }

    @Override
    String namespaceColumn() {
        "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column"
    }

    @Override
    String namespaceTable() {
        "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table"
    }

    @Override
    String namespaceSchema() {
        "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.schema"
    }

    @Override
    String namespaceDatabase() {
        "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver"
    }

    @Override
    DefaultDataTypeProvider getDefaultDataTypeProvider() {
        sqlServerDataTypeProviderService
    }

    @Override
    QueryStringProvider createQueryStringProvider() {
        new SqlServerQueryStringProvider()
    }


    @Override
    SamplingStrategy createSamplingStrategy(String schema, String table, SqlServerDatabaseDataModelImporterProviderServiceParameters parameters) {
        new SqlServerSamplingStrategy(schema, table, parameters)
    }

    @Override
    CalculationStrategy createCalculationStrategy(SqlServerDatabaseDataModelImporterProviderServiceParameters parameters) {
        new SqlServerCalculationStrategy(parameters)
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

    @Override
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
            final DataModel dataModel = importDataModelFromResults(currentUser, folder, schema, parameters.databaseDialect, schemaResults, false,
                                                                   parameters.getListOfTableRegexesToIgnore())
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
    @SuppressWarnings('SqlResolve')
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
            }?.addToMetadata(namespace, row.metadata_key as String, row.metadata_value as String, dataModel.createdBy)
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
