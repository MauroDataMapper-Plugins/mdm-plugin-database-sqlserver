package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.parameters.SqlServerDatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Requires

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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


    void 'OUH04 : test Connection To Ouh for SM'() {
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
            summaryMetadataUseSampling = true
            summaryMetadataSampleThreshold = 100000
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

    void 'OUHXX : test Performance And Export As Json'() {
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
            calculateSummaryMetadata = true
            summaryMetadataSampleThreshold = 100000
            summaryMetadataSamplePercent = 10
            ignoreColumnsForSummaryMetadata = 'id,ID,.+_id,.+_identifier,pat,setgroup,request'
        }
        when:
        long startTime = System.currentTimeMillis()
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)
        log.info('Import complete in {}', Utils.timeTaken(startTime))

        then:
        lims

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, lims)
        Path p = Paths.get('build/export')
        Files.createDirectories(p)
        Path f = p.resolve('modules.json')
        Files.write(f, baos.toByteArray())

        then:
        true
    }

    void 'OUHYY : test Connection To Ouh for enumeration detection'() {
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
//                'demographics,' +
//                'diagnosis,' +
//                'hospital_events,' +
//                'icnarc,' +
//                'icu,' +
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
            ignoreColumnsForEnumerations = 'id,ID,.+_id,.+_identifier,pat,setgroup,request,source,.+_details?,(mrn|nhs)_number'
            ignoreTablesForImport = 'vw_.+'
            enumerationValueUseSampling = true
            enumerationValueSampleThreshold = 20000000
            enumerationValueSamplePercent = 1
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
}
