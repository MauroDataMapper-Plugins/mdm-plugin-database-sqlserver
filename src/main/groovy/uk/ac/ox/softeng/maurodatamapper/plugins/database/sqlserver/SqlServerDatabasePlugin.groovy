package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin

/**
 * @since 17/08/2017
 */
class SqlServerDatabasePlugin extends AbstractMauroDataMapperPlugin {
    @Override
    String getName() {
        return "Plugin:Database - SQLServer"
    }

    @Override
    Closure doWithSpring() {
        {->
            sqlServerDatabaseImporterService(SqlServerDatabaseDataModelImporterProviderService)
            sqlServerDefaultDataTypeProvider(SqlServerDataTypeProvider)
        }
    }
}
