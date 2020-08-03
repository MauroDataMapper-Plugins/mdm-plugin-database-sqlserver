package ox.softeng.metadatacatalogue.plugins.database.sqlserver;

import ox.softeng.metadatacatalogue.core.spi.importer.parameter.config.ImportGroupConfig;
import ox.softeng.metadatacatalogue.core.spi.importer.parameter.config.ImportParameterConfig;
import ox.softeng.metadatacatalogue.plugins.database.DatabaseImportParameters;

import com.google.common.base.Strings;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import net.sourceforge.jtds.jdbcx.JtdsDataSource;

import java.util.Properties;

/**
 * Created by james on 31/05/2017.
 */
public class SqlServerDatabaseImportParameters extends DatabaseImportParameters<JtdsDataSource> {

    @ImportParameterConfig(
        displayName = "Domain Name",
        description = "User domain name. This should be used rather than prefixing the username with <DOMAIN>/<username>.",
        optional = true,
        group = @ImportGroupConfig(
            name = "Database Connection Details",
            order = 1
        )
    )
    private String domain;
    @ImportParameterConfig(
        displayName = "Import Schemas as Separate DataModels",
        description = "Import the schemas found (or defined) as individual DataModels. Each schema DataModel will be imported with the name of the " +
                      "schema.",
        optional = true,
        order = 3,
        group = @ImportGroupConfig(
            name = "Database Import Details",
            order = 2
        )
    )
    private Boolean importSchemasAsSeparateModels;
    @ImportParameterConfig(
        displayName = "Database Schema/s",
        description = "A comma-separated list of the schema names to import.If not supplied then all schemas other than 'sys' and " +
                      "'INFORMATION_SCHEMA' will be imported.",
        optional = true,
        order = 2,
        group = @ImportGroupConfig(
            name = "Database Import Details",
            order = 2
        )
    )
    private String schemaNames;
    @ImportParameterConfig(
        displayName = "SQL Server Instance",
        description = "The name of the SQL Server Instance. This only needs to be supplied if the server is running an instance with a different " +
                      "name to the server.",
        optional = true,
        order = 2,
        group = @ImportGroupConfig(
            name = "Database Connection Details",
            order = 1
        )
    )
    private String serverInstance;
    @ImportParameterConfig(
        displayName = "Use NTLMv2",
        description = "Whether to use NLTMv2 when connecting to the database. Default is false.",
        optional = true,
        group = @ImportGroupConfig(
            name = "Database Connection Details",
            order = 1
        )
    )
    private Boolean useNtlmv2;

    @Override
    public int getDefaultPort() {
        return 1433;
    }

    @Override
    public String getDatabaseDialect() {
        return "MS SQL Server";
    }

    public String getSchemaNames() {
        return schemaNames;
    }

    public void setSchemaNames(String schemaNames) {
        this.schemaNames = schemaNames;
    }

    @Override
    public JtdsDataSource getDataSource(String databaseName) {
        return getJtdsDataSource(databaseName);
    }

    @Override
    public String getUrl(String databaseName) {
        return "UNKNOWN";
    }

    @Override
    public void populateFromProperties(Properties properties) {
        super.populateFromProperties(properties);
        schemaNames = properties.getProperty("import.database.schemas");
        useNtlmv2 = Boolean.parseBoolean(properties.getProperty("import.database.jtds.useNtlmv2"));
        domain = properties.getProperty("import.database.jtds.domain");
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getImportSchemasAsSeparateModels() {
        if (importSchemasAsSeparateModels != null)
            return importSchemasAsSeparateModels;
        else
            return false;
    }

    public void setUseNtlmv2(Boolean useNtlmv2) {
        this.useNtlmv2 = useNtlmv2;
    }

    public void setImportSchemasAsSeparateModels(Boolean importSchemasAsSeparateModels) {
        this.importSchemasAsSeparateModels = importSchemasAsSeparateModels;
    }

    public String getServerInstance() {
        return serverInstance;
    }

    public void setServerInstance(String serverInstance) {
        this.serverInstance = serverInstance;
    }

    public Boolean getUseNtlmv2() {
        if (useNtlmv2 != null)
            return useNtlmv2;
        else
            return false;
    }

    private JtdsDataSource getJtdsDataSource(String databaseName) {
        JtdsDataSource dataSource = new JtdsDataSource();
        dataSource.setServerName(getDatabaseHost());
        dataSource.setPortNumber(getDatabasePort());
        dataSource.setDatabaseName(databaseName);

        if (!Strings.isNullOrEmpty(getServerInstance())) dataSource.setInstance(getServerInstance());
        if (getUseNtlmv2()) dataSource.setUseNTLMV2(getUseNtlmv2());
        if (getDomain() != null) dataSource.setDomain(getDomain());

        getLogger().debug("DataSource connection url using JTDS [NTLMv2: {}, Domain: {}]", getUseNtlmv2(), getDomain());

        return dataSource;
    }

    /**
     * There seem to be issues connecting to the Oxnet mssql servers using the sqlserver driver. However the JTDS one works, as such we've replaced
     * the driver. Leaving this method in here as we may provide an option in the future to use either driver.
     */
    private SQLServerDataSource getSqlServerDataSource(String databaseName) {
        SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setServerName(getDatabaseHost());
        dataSource.setPortNumber(getDatabasePort());
        dataSource.setDatabaseName(databaseName);

        if (getDatabaseSSL()) {
            dataSource.setEncrypt(true);
            dataSource.setTrustServerCertificate(true);
        }

        //dataSource.setIntegratedSecurity(true);
        //dataSource.setAuthenticationScheme("JavaKerberos");

        getLogger().info("DataSource connection using SQLServer");

        return dataSource;
    }
}
