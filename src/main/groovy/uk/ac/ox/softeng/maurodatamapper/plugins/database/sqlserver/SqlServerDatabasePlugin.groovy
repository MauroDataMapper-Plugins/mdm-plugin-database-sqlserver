package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin

class SqlServerDatabasePlugin extends AbstractMauroDataMapperPlugin {

    @Override
    String getName() {
        'Plugin : Database - SQLServer'
    }

    @Override
    Closure doWithSpring() {
        {->
            sqlServerDatabaseDataModelImporterProviderService SqlServerDatabaseDataModelImporterProviderService
            sqlServerDataTypeProvider SqlServerDataTypeProvider
        }
    }
}
