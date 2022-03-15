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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.parameters.SqlServerDatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Requires

import java.nio.file.Files

/**
 * @since 08/03/2022
 */
@Requires({
    getSys().containsKey('database.username') && getSys().containsKey('database.password')
})
@Slf4j
@Integration
@Rollback
class RemoteDatabaseImporterProviderServiceSpec extends BaseDatabasePluginTest<
    SqlServerDatabaseDataModelImporterProviderServiceParameters,
    SqlServerDatabaseDataModelImporterProviderService> {


    SqlServerDatabaseDataModelImporterProviderService sqlServerDatabaseDataModelImporterProviderService
    DataModelJsonExporterService dataModelJsonExporterService

    @Override
    SqlServerDatabaseDataModelImporterProviderServiceParameters createDatabaseImportParameters() {
        new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseNames = 'msdb'
            databaseUsername = 'sa'
            databasePassword = 'yourStrong(!)Password'
            authenticationScheme = 'nativeAuthentication'
            integratedSecurity = false
        }
    }

    @Override
    String getDatabasePortPropertyName() {
        'unknown'
    }

    @Override
    int getDefaultDatabasePort() {
        1433
    }

    @Override
    SqlServerDatabaseDataModelImporterProviderService getImporterInstance() {
        sqlServerDatabaseDataModelImporterProviderService
    }

    void 'OUH01 : test Connection To Ouh for enumeration detection'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims
        lims.enumerationTypes.size() == 4

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('lims.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }

    void 'OUH02 : test Connection To Ouh for enumeration detection with sampling threshold but sampling disabled'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
            enumerationValueSampleThreshold = 100000
            enumerationValueSamplePercent = 20
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims
        lims.enumerationTypes.size() == 0

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('lims.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }

    void 'OUH03 : test Connection To Ouh for enumeration detection with sampling threshold and sampling enabled'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
            enumerationValueSampleThreshold = 100000
            enumerationValueSamplePercent = 20
            enumerationValueUseSampling = true
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims
        lims.enumerationTypes.size() == 5

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('lims.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }


    void 'OUH04 : test Connection To Ouh for SM only'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            //            detectEnumerations = true
            //            maxEnumerations = 20
            //            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
            //            enumerationValueUseSampling = true
            //            enumerationValueSampleThreshold = 100000
            //            enumerationValueSamplePercent = 100
            calculateSummaryMetadata = true
            summaryMetadataUseSampling = true
            summaryMetadataSampleThreshold = 2000000
            summaryMetadataSamplePercent = 10
            ignoreColumnsForSummaryMetadata = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('lims.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }

    void 'OUH05 : test Connection To Ouh for SM and EV'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
            enumerationValueUseSampling = true
            enumerationValueSampleThreshold = 2000000
            enumerationValueSamplePercent = 50
            calculateSummaryMetadata = true
            summaryMetadataUseSampling = true
            summaryMetadataSampleThreshold = 2000000
            summaryMetadataSamplePercent = 10
            ignoreColumnsForSummaryMetadata = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('lims.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }

    void 'OUHYY : test complex Connection To Ouh'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp02.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = System.getProperty('database.username')
            databasePassword = System.getProperty('database.password')
            databaseNames = 'modules'
            schemaNames =
                'demographics,' +
                'diagnosis,' +
                'hospital_events,' +
                'icnarc,' +
                'icu,' +
                'laboratory_tests,' +
                'medical_scoring,' +
                'medications,' +
                'observations,' +
                'patient_history,' +
                'procedures,' +
                'reference,' +
                'reference_2,' +
                'reports,' +
                'risk_factors,' +
                'theatre'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            ignoreTablesForImport = 'vw_.+,v_.+,' +
                                    'grouped_alcohol_status,' +
                                    'clinical_events_orbit,' +
                                    'all_alcohol_status_observations,' +
                                    'laboratory_tests_biochemistry_code_breakdown,' +
                                    'laboratory_tests_biochemistry_name_breakdown,' +
                                    'clinical_events_orbit_OLD'
            detectEnumerations = true
            maxEnumerations = 20
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,source,.+_details?,(mrn|nhs)_number'
            enumerationValueUseSampling = true
            enumerationValueSampleThreshold = 2000000
            enumerationValueSamplePercent = 50
            calculateSummaryMetadata = true
            summaryMetadataUseSampling = true
            summaryMetadataUseDynamicSamplePercent = true
            summaryMetadataSampleThreshold = 2000000
            summaryMetadataSamplePercent = 10
            ignoreColumnsForSummaryMetadata = 'id,ID,.+_id,.+_identifier,source,.+_details?,(mrn|nhs)_number'
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims
        lims.enumerationTypes.size() > 1

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Files.write(resourcesPath.resolve('modules.json'), baos.toByteArray())

        then:
        noExceptionThrown()
    }
}
