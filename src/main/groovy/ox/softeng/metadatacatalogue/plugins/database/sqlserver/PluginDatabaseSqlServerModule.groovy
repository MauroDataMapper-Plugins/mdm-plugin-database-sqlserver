package ox.softeng.metadatacatalogue.plugins.database.sqlserver

import ox.softeng.metadatacatalogue.core.spi.module.AbstractModule

/**
 * @since 17/08/2017
 */
class PluginDatabaseSqlServerModule extends AbstractModule {
    @Override
    String getName() {
        return "Plugin:Database - SQLServer"
    }

    @Override
    Closure doWithSpring() {
        {->
            sqlServerDatabaseImporterService(SqlServerDatabaseImporterService)
            sqlServerDefaultDataTypeProvider(SqlServerDefaultDataTypeProvider)
        }
    }
}