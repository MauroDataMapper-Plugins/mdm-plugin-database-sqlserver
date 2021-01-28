/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.RemoteDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

import java.sql.Connection
import java.sql.PreparedStatement

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
        '3.0.0-SNAPSHOT'
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
