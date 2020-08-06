package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters

import com.microsoft.sqlserver.jdbc.SQLServerDataSource
import net.sourceforge.jtds.jdbcx.JtdsDataSource

import groovy.util.logging.Slf4j

@Slf4j
// @CompileStatic
class SqlServerDatabaseDataModelImporterProviderServiceParameters extends DatabaseDataModelImporterProviderServiceParameters<JtdsDataSource> {

    @ImportParameterConfig(
        displayName = 'Domain Name',
        description = 'User domain name. This should be used rather than prefixing the username with <DOMAIN>/<username>.',
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Connection Details',
            order = 1
        )
    )
    String domain

    @ImportParameterConfig(
        displayName = 'Import Schemas as Separate DataModels',
        description = [
            'Import the schemas found (or defined) as individual DataModels.',
            'Each schema DataModel will be imported with the name of the schema.'],
        order = 3,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Import Details',
            order = 2
        )
    )
    Boolean importSchemasAsSeparateModels

    @ImportParameterConfig(
        displayName = 'Database Schema/s',
        description = [
            'A comma-separated list of the schema names to import.',
            'If not supplied then all schemas other than "sys" and "INFORMATION_SCHEMA" will be imported.'],
        order = 2,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Import Details',
            order = 2
        )
    )
    String schemaNames

    @ImportParameterConfig(
        displayName = 'SQL Server Instance',
        description = [
            'The name of the SQL Server Instance.',
            'This only needs to be supplied if the server is running an instance with a different name to the server.'],
        order = 2,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Connection Details',
            order = 1
        )
    )
    String serverInstance

    @ImportParameterConfig(
        displayName = 'Use NTLMv2',
        description = 'Whether to use NLTMv2 when connecting to the database. Default is false.',
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Connection Details',
            order = 1
        )
    )
    Boolean useNtlmv2

    boolean getImportSchemasAsSeparateModels() {
        importSchemasAsSeparateModels ?: false
    }

    boolean getUseNtlmv2() {
        useNtlmv2 ?: false
    }

    @Override
    void populateFromProperties(Properties properties) {
        super.populateFromProperties properties
        domain = properties.getProperty 'import.database.jtds.domain'
        schemaNames = properties.getProperty 'import.database.schemas'
        useNtlmv2 = properties.getProperty('import.database.jtds.useNtlmv2') as Boolean
    }

    @Override
    JtdsDataSource getDataSource(String databaseName) {
        log.debug 'DataSource connection url using JTDS [NTLMv2: {}, Domain: {}]', getUseNtlmv2(), domain
        new JtdsDataSource().tap {
            setServerName databaseHost
            setPortNumber databasePort
            setDatabaseName databaseName
            if (domain) setDomain domain
            if (serverInstance) setInstance serverInstance
            if (getUseNtlmv2()) setUseNTLMV2 getUseNtlmv2()
        }
    }

    @Override
    String getUrl(String databaseName) {
        'UNKNOWN'
    }

    @Override
    String getDatabaseDialect() {
        'MS SQL Server'
    }

    @Override
    int getDefaultPort() {
        1433
    }

    /**
     * There seem to be issues connecting to the Oxnet mssql servers using the sqlserver driver. However the JTDS one works, as such we've replaced
     * the driver. Leaving this method in here as we may provide an option in the future to use either driver.
     */
    @SuppressWarnings('UnusedPrivateMethod')
    private SQLServerDataSource getSqlServerDataSource(String databaseName) {
        log.debug 'DataSource connection using SQLServer'
        new SQLServerDataSource().tap {
            setServerName databaseHost
            setPortNumber databasePort
            setDatabaseName databaseName
            if (databaseSSL) {
                setEncrypt true
                setTrustServerCertificate true
            }
        }
    }
}
