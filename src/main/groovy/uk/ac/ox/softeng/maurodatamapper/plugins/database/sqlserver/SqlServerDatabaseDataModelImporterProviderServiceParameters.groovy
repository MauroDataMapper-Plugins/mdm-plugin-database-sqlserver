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

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters

import com.microsoft.sqlserver.jdbc.SQLServerDataSource
import groovy.util.logging.Slf4j

@Slf4j
// @CompileStatic
class SqlServerDatabaseDataModelImporterProviderServiceParameters extends DatabaseDataModelImporterProviderServiceParameters<SQLServerDataSource> {


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
        ))
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
        ))
    String schemaNames

    @ImportParameterConfig(
        displayName = 'Authentication Scheme',
        description = ['Authentication scheme to use, options are [nativeAuthentication, ntlm, javaKerberos].',
            'If anything other than nativeAuthentication is used integratedSecurity will be set to "true". Default is NTLM.'],
        order = 1,
        group = @ImportGroupConfig(
            name = 'SQLServer DataSource Connection Details',
            order = 1
        ))
    String authenticationScheme

    @ImportParameterConfig(
        displayName = 'Integrated Security',
        description = ['Use integrated security?',
            'If anything other than nativeAuthentication is used as authentication schema then integratedSecurity will be set to "true".'],
        order = 2,
        group = @ImportGroupConfig(
            name = 'SQLServer DataSource Connection Details',
            order = 1
        ))
    Boolean integratedSecurity

    @ImportParameterConfig(
        displayName = 'Domain Name',
        description = 'User domain name. This should be used rather than prefixing the username with <DOMAIN>/<username>.',
        order = 3,
        optional = true,
        group = @ImportGroupConfig(
            name = 'SQLServer DataSource Connection Details',
            order = 1
        ))
    String domain

    @ImportParameterConfig(
        displayName = 'SQL Server Instance',
        description = [
            'The name of the SQL Server Instance.',
            'This only needs to be supplied if the server is running an instance with a different name to the server hostname.'],
        order = 4,
        optional = true,
        group = @ImportGroupConfig(
            name = 'SQLServer DataSource Connection Details',
            order = 1
        ))
    String serverInstance

    @ImportParameterConfig(
        displayName = 'Sample Threshold',
        description = [
            'Use sampling if the number of rows in a table exceeds this threshold. Set the value to 0 to ',
            'never sample. If no value is supplied, then 0 is assumed. Sampling is done using the SQL Server TABLESAMPLE clause.'],
        order = 7,
        optional = true,
        group = @ImportGroupConfig(
            name = 'Database Import Details',
            order = 2
        )
    )
    Integer sampleThreshold = 0

    @ImportParameterConfig(
        displayName = 'Sample Percentage',
        description = [
            'If sampling, the percentage of rows to use as a sample. If the sampling threshold is > 0 but no',
            'value is supplied for Sample Percentage, a default value of 1% will be used.'
        ],
        order = 8,
        optional = false,
        group = @ImportGroupConfig(
            name = 'Database Import Details',
            order = 2
        )
    )
    BigDecimal samplePercent = 1

    boolean getImportSchemasAsSeparateModels() {
        importSchemasAsSeparateModels ?: false
    }

    @Override
    void populateFromProperties(Properties properties) {
        super.populateFromProperties properties
        schemaNames = properties.getProperty 'import.database.schemas'
    }

    @Override
    SQLServerDataSource getDataSource(String databaseName) {
        getSqlServerDataSource(databaseName)
    }

    @Override
    String getUrl(String databaseName) {
        getDataSource(databaseName).getURL()
    }

    @Override
    String getDatabaseDialect() {
        'MS SQL Server'
    }

    @Override
    int getDefaultPort() {
        1433
    }

    String getProvidedDomain(){
        domain
    }

    SQLServerDataSource getSqlServerDataSource(String databaseName) {
        log.debug 'DataSource connection using SQLServer'
        SQLServerDataSource ds = new SQLServerDataSource().tap {
            setServerName databaseHost
            setPortNumber databasePort
            setDatabaseName databaseName
            setTrustServerCertificate true

            String authScheme = getAuthenticationScheme() ?: 'ntlm'
            if (!(authScheme.toLowerCase() in ['nativeauthentication', 'ntlm', 'javakerberos'])) authScheme = 'ntlm'
            setAuthenticationScheme(authScheme)

            if (authScheme.toLowerCase() == 'nativeauthentication') {
                setIntegratedSecurity getIntegratedSecurity()
            } else {
                setIntegratedSecurity true
            }

            if (serverInstance) setInstanceName(serverInstance)
            if (databaseSSL) {
                setEncrypt true
            }
            setApplicationName('Mauro-Data-Mapper')
            if (getProvidedDomain()) setDomain(getProvidedDomain())
        }
    }
}
