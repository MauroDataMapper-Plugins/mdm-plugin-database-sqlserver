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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.AbstractDatabaseDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Transactional
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.FieldError

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

/**
 * @since 08/08/2017
 */
@Slf4j
@Transactional
abstract class BaseDatabasePluginTest<P extends DatabaseDataModelImporterProviderServiceParameters,
    T extends AbstractDatabaseDataModelImporterProviderService<P>> extends BaseIntegrationSpec  implements JsonComparer{

    Path resourcesPath
    int databasePort
    String databaseHost
    Authority testAuthority

    @Autowired
    DataModelService dataModelService

    @Autowired
    ImporterService importerService

    abstract protected P createDatabaseImportParameters()

    abstract protected String getDatabasePortPropertyName()

    abstract protected int getDefaultDatabasePort()

    abstract T getImporterInstance()

    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources')
        try {
            databasePort = Integer.parseInt(System.getProperty(getDatabasePortPropertyName()))
        } catch (Exception ignored) {
            databasePort = getDefaultDatabasePort()
        }
        databaseHost = 'localhost'
    }

    @Override
    void setupDomainData() {
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
        testAuthority = Authority.findByLabel('Test Authority')
        assert testAuthority
        checkAndSave(testAuthority)
    }

    void 'test Import Database'() {
        given:
        setupData()
        P params = createDatabaseImportParameters(databaseHost, databasePort)

        expect:
        importDataModelAndRetrieveFromDatabase(params)
    }

    protected P createDatabaseImportParameters(String host, int port) {
        P params = createDatabaseImportParameters()
        params.setDatabaseHost(host)
        params.setDatabasePort(port)
        params.setFinalised(false)
        params.setFolderId(getFolder().getId())
        params.setDatabaseSSL(false)
        params.setImportAsNewDocumentationVersion(false)
        params.setDataModelNameSuffix('')
        params
    }

    protected DataModel importDataModelAndRetrieveFromDatabase(P params) {

        def errors = importerService.validateParameters(params, getImporterInstance().importerProviderServiceParametersClass)

        if (errors.hasErrors()) {
            errors.allErrors.each {error ->

                String msg = messageSource ? messageSource.getMessage(error, Locale.default) :
                             "${error.defaultMessage} :: ${Arrays.asList(error.arguments)}"

                if (error instanceof FieldError) msg += " :: [${error.field}]"

                log.error msg
                System.err.println msg
            }
            fail('Import parameters are not valid')
        }

        try {
            getImporterInstance().getConnection(params.databaseNames, params)
        } catch (ApiException e) {
            fail(e.getMessage())
        }

        DataModel importedModel = importDomain(params)

        log.debug('Getting datamodel {} from database to verify', importedModel.getId())
        sessionFactory.getCurrentSession().clear()
        // Rather than use the one returned from the import, we want to check whats actually been saved into the DB
        DataModel dataModel = DataModel.get(importedModel.getId())
        assertNotNull('DataModel should exist in Database', dataModel)
        dataModel
    }

    protected DataModel importDomain(P params) {
        importDomain(params, true)
    }

    protected DataModel importDomain(P params, boolean validate) {
        long startTime = System.currentTimeMillis()

        log.debug('Importing using {}', getImporterInstance().getDisplayName())
        DataModel importedModel = getImporterInstance().importDomain(getEditor(), params)

        if (importedModel instanceof Model) {
            importedModel.folder = folder
        }

        long endTime = System.currentTimeMillis()
        log.info('Import complete in {}', Utils.getTimeString(endTime - startTime))

        assertNotNull('Domain should be imported', importedModel)

        if (validate) {
            log.info('Validating imported model')
            if (dataModelService.validate(importedModel)) {
                log.info('Saving valid imported model')
            } else {
                GormUtils.outputDomainErrors(getMessageSource(), importedModel)
                fail('Domain is invalid')
            }
        }

        log.info('Flushing current session')
        sessionFactory.getCurrentSession().flush()

        log.info('Completed importing domain')
        return importedModel
    }

    protected List<DataModel> importDomains(P params, int expectedSize, boolean validate) {
        try {

            if (!getImporterInstance().canImportMultipleDomains()) {
                fail("Importer [${getImporterInstance().getDisplayName()}] cannot handle importing multiple domains")
            }

            long startTime = System.currentTimeMillis()

            log.debug('Importing {}', getImporterInstance().getDisplayName())
            List<DataModel> importedModels = getImporterInstance().importDomains(getEditor(), params)

            importedModels.each {importedModel ->
                if (importedModel instanceof Model) {
                    importedModel.folder = folder
                }
            }

            long endTime = System.currentTimeMillis()
            log.info('Import complete in {}', Utils.getTimeString(endTime - startTime))

            assertNotNull('Domains should be imported', importedModels)
            assertEquals('Number of domains imported', expectedSize, importedModels.size())

            if (validate) {
                importedModels.each {domain ->
                    if (!domain.validate()) {
                        GormUtils.outputDomainErrors(getMessageSource(), domain)
                        fail('Domain is invalid')
                    }
                }
            }
            sessionFactory.getCurrentSession().flush()
            return importedModels
        } catch (Exception ex) {
            log.error('Something went wrong importing', ex)
            fail(ex.getMessage())
        }
        Collections.emptyList()
    }

    protected List<DataModel> importDomains(P params) {
        importDomains(params, 1)
    }

    protected List<DataModel> importDomains(P params, int expectedSize) {
        importDomains(params, expectedSize, true)
    }


    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.json")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }
}
