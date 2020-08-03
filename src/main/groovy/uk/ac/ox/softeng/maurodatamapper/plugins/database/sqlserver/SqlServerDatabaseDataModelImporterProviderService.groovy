package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.RemoteDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Created by james on 31/05/2017.
 */
class SqlServerDatabaseDataModelImporterProviderService extends AbstractDatabaseDataModelImporterProviderService<SqlServerDatabaseDataModelImporterProviderServiceParameters>
        implements RemoteDatabaseDataModelImporterProviderService {

    @Override
    String getDatabaseStructureQueryString() {
        'SELECT * FROM INFORMATION_SCHEMA.COLUMNS;'
    }

    @Override
    String getDisplayName() {
        'MS SQL Server Importer'
    }

    @Override
    String getVersion() {
        '2.1.0'
    }

    @Override
    void updateDataModelWithDatabaseSpecificInformation(DataModel dataModel, Connection connection) {

        addStandardConstraintInformation(dataModel, connection)
        addPrimaryKeyAndUniqueConstraintInformation(dataModel, connection)
        addIndexInformation(dataModel, connection)
        addForeignKeyInformation(dataModel, connection)

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
'''
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
'''
    }

    @Override
    PreparedStatement prepareCoreStatement(Connection connection, SqlServerDatabaseDataModelImporterProviderServiceParameters params) {
        PreparedStatement st
        if (params.schemaNames) {
            List<String> names = params.schemaNames.split(',')
            String sb = """SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA IN (${names.collect {'?'}.join(',')});"""
            st = connection.prepareStatement(sb)
            names.eachWithIndex {String entry, int i ->
                st.setString(i + 1, entry)
            }
            return st
        }
        super.prepareCoreStatement(connection, params)
    }

    @Override
    List<DataModel> importAndUpdateDataModelsFromResults(User currentUser, String databaseName, SqlServerDatabaseDataModelImporterProviderServiceParameters params,
                                                         Folder folder,
                                                         String modelName, List<Map<String, Object>> results, Connection connection) {
        if (!params.getImportSchemasAsSeparateModels()) {
            return super.importAndUpdateDataModelsFromResults(currentUser, databaseName, params,
                                                              folder, modelName, results, connection)
        }

        getLogger().debug('Importing all schemas as separate DataModels')
        Map<String, List<Map<String, Object>>> groupedSchemas = results.groupBy {row ->
            row.get(getSchemaNameColumnName()) as String
        }

        groupedSchemas.collect {schema, schemaResults ->
            getLogger().debug('Importing database {} schema {}', databaseName, schema)
            DataModel dataModel = importDataModelFromResults(currentUser, folder, schema, params.getDatabaseDialect(), schemaResults, false)
            if (params.dataModelNameSuffix) dataModel.aliasesString = databaseName
            updateDataModelWithDatabaseSpecificInformation(dataModel, connection)
            dataModel
        }
    }
}
