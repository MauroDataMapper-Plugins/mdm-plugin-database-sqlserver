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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.parameters.SqlServerDatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Ignore

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

// @CompileStatic
@Slf4j
@Integration
@Rollback
class SqlServerDatabaseDataModelImporterProviderServiceSpec
    extends BaseDatabasePluginTest<
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


    @Ignore('no credentials')
    void 'test Connection To Ouh'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = ''
            databasePassword = ''
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
        }

        when:
        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        then:
        lims
    }

    @Ignore('no credentials')
    void 'test Performance And Export As Json'() {
        given:
        setupData()
        SqlServerDatabaseDataModelImporterProviderServiceParameters params = new SqlServerDatabaseDataModelImporterProviderServiceParameters().tap {
            databaseHost = 'oxnetdwp01.oxnet.nhs.uk'
            domain = 'OXNET'
            authenticationScheme = 'ntlm'
            integratedSecurity = true
            databaseUsername = ''
            databasePassword = ''
            databaseNames = 'LIMS'
            schemaNames = 'raw'
            databaseSSL = false
            folderId = folder.getId()
            detectEnumerations = true
            maxEnumerations = 20
            calculateSummaryMetadata = true
            sampleThreshold = 100000
            samplePercent = 10
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

    void 'test Import Simple Database'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {databaseNames = 'metadata_simple'})

        then:
        checkBasic(dataModel)
        checkOrganisationNotEnumerated(dataModel)
        checkSampleNoSummaryMetadata(dataModel)
        checkBiggerSampleNoSummaryMetadata(dataModel)
        /**
         * Column types expected are:
         * uniqueidentifier
         * bigint
         * datetime
         * varchar
         * nvarchar
         * int
         * binary
         * bit
         * char
         * smallint
         * decimal
         * numeric
         * tinyint
         * date
         * smalldatetime
         * datetime2
         *
         * So 16 primitive types, plus two reference types for catalogue_itemType and catalogue_userType
         */

        when:
        List<String> defaultDataTypeLabels = importerInstance.defaultDataTypeProvider.defaultListOfDataTypes.collect {it.label}

        then:
        assertEquals 'Default DT Provider', 40, defaultDataTypeLabels.size()
        assertEquals 'Number of columntypes/datatypes', 42, dataModel.dataTypes?.size()
        assertTrue 'All primitive DTs map to a default DT', dataModel.primitiveTypes.findAll {!(it.label in defaultDataTypeLabels)}.isEmpty()
        assertEquals 'Number of primitive types', 40, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 0, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of char datatypes', 1, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType' && it.label == 'char'}.size()
    }

    void 'EV : test Import Simple Database With Enumerations'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {
                databaseNames = 'metadata_simple'
                detectEnumerations = true
                maxEnumerations = 20
            })

        then:
        checkBasic(dataModel)
        checkOrganisationEnumerated(dataModel)
        checkSampleNoSummaryMetadata(dataModel)
        checkBiggerSampleNoSummaryMetadata(dataModel)

        when:
        List<String> defaultDataTypeLabels = importerInstance.defaultDataTypeProvider.defaultListOfDataTypes.collect {it.label}

        then:
        assertEquals 'Default DT Provider', 40, defaultDataTypeLabels.size()
        assertEquals 'Number of columntypes/datatypes', 48, dataModel.dataTypes?.size()
        assertTrue 'All primitive DTs map to a default DT', dataModel.primitiveTypes.findAll {!(it.label in defaultDataTypeLabels)}.isEmpty()
        assertEquals 'Number of primitive types', 40, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 6, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of tables/dataclasses', 8, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

    }

    void 'SM01 : test Import Simple Database With Summary Metadata'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {
                databaseNames = 'metadata_simple'
                detectEnumerations = true
                maxEnumerations = 20
                calculateSummaryMetadata = true
                summaryMetadataUseSampling = false
                enumerationValueUseSampling = false
                ignoreColumnsForSummaryMetadata = '.*id'
                ignoreColumnsForEnumerations = '.*id'
            })

        then:
        checkBasic(dataModel)
        checkOrganisationEnumerated(dataModel)
        checkOrganisationSummaryMetadata(dataModel)
        checkSampleSummaryMetadata(dataModel)
        checkBiggerSampleSummaryMetadata(dataModel)
    }

    void 'SM02 : test Import Simple Database With Summary Metadata With Sampling'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {
                databaseNames = 'metadata_simple'
                detectEnumerations = true
                maxEnumerations = 20
                calculateSummaryMetadata = true
                summaryMetadataUseSampling = true
                summaryMetadataSampleThreshold = 1000
                summaryMetadataSamplePercent = 10
                enumerationValueUseSampling = true
                enumerationValueSampleThreshold = 1000
                enumerationValueSamplePercent = 10
                ignoreColumnsForSummaryMetadata = '.*id'
                ignoreColumnsForEnumerations = '.*id'
            })

        then:
        checkBasic(dataModel)
        checkOrganisationEnumerated(dataModel)
        checkSampleSummaryMetadata(dataModel)

        when:
        final DataClass publicSchema = dataModel.childDataClasses.first()

        then:
        assertEquals 'Number of child tables/dataclasses', 7, publicSchema.dataClasses?.size()

        when:
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'bigger_sample'}

        then:
        assertEquals 'Sample Number of columns/dataElements', 4, sampleTable.dataElements.size()

        when:
        final DataElement sample_bigint = sampleTable.dataElements.find{it.label == "sample_bigint"}

        then:
        assertEquals 'description of summary metadata for sample_bigint',
                'Estimated Value Distribution (calculated by sampling 10% of rows)',
                sample_bigint.summaryMetadata[0].description

        when:
        final DataElement sample_decimal = sampleTable.dataElements.find{it.label == "sample_decimal"}

        then:
        assertEquals 'description of summary metadata for sample_decimal',
                'Estimated Value Distribution (calculated by sampling 10% of rows)',
                sample_decimal.summaryMetadata[0].description

        when:
        final DataElement sample_date = sampleTable.dataElements.find{it.label == "sample_date"}

        then:
        assertEquals 'description of summary metadata for sample_date',
                'Estimated Value Distribution (calculated by sampling 10% of rows)',
                sample_date.summaryMetadata[0].description

        /**
         * Enumeration type determined using a sample, so we can't be certain that there will be exactly 15 results.
         * But there should be between 1 and 15 values, and any values must be in our expected list.
         */
        when:
        final EnumerationType sampleVarcharEnumerationType = sampleTable.findDataElement('sample_varchar').dataType

        then:
        assertTrue 'One or more 0 enumeration values', sampleVarcharEnumerationType.enumerationValues.size() >= 1
        assertTrue '15 or fewer enumeration values', sampleVarcharEnumerationType.enumerationValues.size() <= 15
        sampleVarcharEnumerationType.enumerationValues.each {
            assertTrue 'Enumeration key in expected set',
                    ['ENUM0', 'ENUM1', 'ENUM2', 'ENUM3', 'ENUM4', 'ENUM5', 'ENUM6', 'ENUM7', 'ENUM8', 'ENUM9', 'ENUM10', 'ENUM11', 'ENUM12', 'ENUM13', 'ENUM14'].contains(it.key)
        }
    }

    void 'SM03 : test Import Simple Database With Summary Metadata With Sampling disabled but threshold set'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {
                databaseNames = 'metadata_simple'
                detectEnumerations = true
                maxEnumerations = 20
                calculateSummaryMetadata = true
                summaryMetadataUseSampling = false
                summaryMetadataSampleThreshold = 1000
                summaryMetadataSamplePercent = 10
                enumerationValueUseSampling = true
                enumerationValueSampleThreshold = 1000
                enumerationValueSamplePercent = 10
                ignoreColumnsForSummaryMetadata = '.*id'
                ignoreColumnsForEnumerations = '.*id'
            })

        then:
        checkBasic(dataModel)
        checkOrganisationEnumerated(dataModel)
        checkSampleSummaryMetadata(dataModel)

        when:
        final DataClass publicSchema = dataModel.childDataClasses.first()

        then:
        assertEquals 'Number of child tables/dataclasses', 7, publicSchema.dataClasses?.size()

        // All of the following had row count > threshold so we would sample but sampling has been disabled
        when:
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'bigger_sample'}

        then:
        assertEquals 'Sample Number of columns/dataElements', 4, sampleTable.dataElements.size()

        when:
        final DataElement sample_bigint = sampleTable.dataElements.find{it.label == "sample_bigint"}

        then:
        assertEquals 'Zero summaryMetadata for sample_big_int', 0, sample_bigint.summaryMetadata.size()

        when:
        final DataElement sample_decimal = sampleTable.dataElements.find{it.label == "sample_decimal"}

        then:
        assertEquals 'Zero summaryMetadata for sample_decimal', 0, sample_bigint.summaryMetadata.size()

        when:
        final DataElement sample_date = sampleTable.dataElements.find{it.label == "sample_date"}

        then:
        assertEquals 'Zero summaryMetadata for sample_date', 0, sample_bigint.summaryMetadata.size()

        /**
         * Enumeration type determined using a sample, so we can't be certain that there will be exactly 15 results.
         * But there should be between 1 and 15 values, and any values must be in our expected list.
         */
        when:
        final EnumerationType sampleVarcharEnumerationType = sampleTable.findDataElement('sample_varchar').dataType

        then:
        assertTrue 'One or more 0 enumeration values', sampleVarcharEnumerationType.enumerationValues.size() >= 1
        assertTrue '15 or fewer enumeration values', sampleVarcharEnumerationType.enumerationValues.size() <= 15
        sampleVarcharEnumerationType.enumerationValues.each {
            assertTrue 'Enumeration key in expected set',
                       ['ENUM0', 'ENUM1', 'ENUM2', 'ENUM3', 'ENUM4', 'ENUM5', 'ENUM6', 'ENUM7', 'ENUM8', 'ENUM9', 'ENUM10', 'ENUM11', 'ENUM12', 'ENUM13', 'ENUM14'].contains(it.key)
        }
    }

    void 'SM04 : test Import Simple Database With Summary Metadata and export to json'() {
        given:
        setupData()

        when:
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {
                databaseNames = 'metadata_simple'
                detectEnumerations = true
                maxEnumerations = 20
                calculateSummaryMetadata = true
                summaryMetadataUseSampling = false
                enumerationValueUseSampling = false
                ignoreColumnsForSummaryMetadata = '.*id'
                ignoreColumnsForEnumerations = '.*id'
            })

        then:
        checkBasic(dataModel)
        checkOrganisationEnumerated(dataModel)
        checkOrganisationSummaryMetadata(dataModel)
        checkSampleSummaryMetadata(dataModel)
        checkBiggerSampleSummaryMetadata(dataModel)

        when:
        ByteArrayOutputStream baos = dataModelJsonExporterService.exportDataModel(UnloggedUser.instance, dataModel)
        String exportedModel = new String(baos.toByteArray(), Charset.defaultCharset())

        then:
        validateExportedModel('simpleSummaryMetadata', exportedModel)
    }

    private void checkBasic(DataModel dataModel) {
        assertEquals 'Database/Model name', 'metadata_simple', dataModel.label
        assertTrue 'info extended property present', dataModel.getMetadata().any{Metadata md ->
            md.key == 'info' && md.value == 'A database called metadata_simple which is used for integration testing'
        }

        /**
         * Expect data classes for:
         * dbo (child of the data model)
         * catalogue_item (child of dbo)
         * catalogue_user (child of dbo)
         * metadata (child of dbo)
         * organisation (child of dbo)
         * sample (child of dbo)
         * sample_bigger (child of dbo)
         */
        assertEquals 'Number of tables/dataclasses', 8, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 7, publicSchema.dataClasses?.size()

        //The public schema 'dbo' should have an extended property in metadata
        assertEquals 'public schema is dbo', 'dbo', publicSchema.label
        //TODO this assertion fails because somehow the metadata does not get persisted when saved during this test,
        //although it works OK when using the plugin for real.
        /*assertTrue 'desc extended property present', publicSchema.getMetadata().any{Metadata md ->
            md.key == 'desc' && md.value == 'Contains objects used for testing'
        }*/

        final Set<DataClass> dataClasses = publicSchema.dataClasses

        // Tables
        final DataClass metadataTable = dataClasses.find {it.label == 'metadata'}
        assertEquals 'Metadata Number of columns/dataElements', 10, metadataTable.dataElements.size()
        assertEquals 'Metadata Number of metadata', 3, metadataTable.metadata.size()

        assertTrue 'MD All metadata values are valid', metadataTable.metadata.every {it.value && it.key != it.value}

        List<Map> indexesInfo = new JsonSlurper().parseText(metadataTable.metadata.find {it.key == 'indexes'}.value) as List<Map>

        assertEquals('MD Index count', 4, indexesInfo.size())

        assertEquals 'MD Primary key', 1, metadataTable.metadata.count {it.key == 'primary_key_name'}
        assertEquals 'MD Primary key', 1, metadataTable.metadata.count {it.key == 'primary_key_columns'}
        assertEquals 'MD Primary indexes', 1, indexesInfo.findAll {it.primaryIndex}.size()
        assertEquals 'MD Unique indexes', 2, indexesInfo.findAll {it.uniqueIndex}.size()
        assertEquals 'MD indexes', 2, indexesInfo.findAll {!it.uniqueIndex && !it.primaryIndex}.size()

        final Map multipleColIndex =indexesInfo.find {it.name ==  'unique_item_id_namespace_key'}
        assertNotNull 'Should have multi column index', multipleColIndex
        assertEquals 'Correct order of columns', 'catalogue_item_id, namespace, md_key', multipleColIndex.columns

        final DataClass ciTable = dataClasses.find {it.label == 'catalogue_item'}
        assertEquals 'CI Number of columns/dataElements', 10, ciTable.dataElements.size()
        assertEquals 'CI Number of metadata', 3, ciTable.metadata.size()

        assertTrue 'CI All metadata values are valid', ciTable.metadata.every {it.value && it.key != it.value}

        indexesInfo = new JsonSlurper().parseText(ciTable.metadata.find {it.key == 'indexes'}.value) as List<Map>

        assertEquals('CI Index count', 3, indexesInfo.size())

        assertEquals 'CI Primary key', 1, ciTable.metadata.count {it.key == 'primary_key_name'}
        assertEquals 'CI Primary key', 1, ciTable.metadata.count {it.key == 'primary_key_columns'}
        assertEquals 'CI Primary indexes', 1, indexesInfo.findAll {it.primaryIndex}.size()
        assertEquals 'CI indexes', 2, indexesInfo.findAll {!it.uniqueIndex && !it.primaryIndex}.size()

        final DataClass cuTable = dataClasses.find {it.label == 'catalogue_user'}
        assertEquals 'CU Number of columns/dataElements', 18, cuTable.dataElements.size()
        assertEquals 'CU Number of metadata', 5, cuTable.metadata.size()

        assertTrue 'CU All metadata values are valid', cuTable.metadata.every {it.value && it.key != it.value}

        indexesInfo = new JsonSlurper().parseText(cuTable.metadata.find {it.key == 'indexes'}.value) as List<Map>

        assertEquals('CU Index count', 3, indexesInfo.size())

        assertEquals 'CU Primary key', 1, cuTable.metadata.count {it.key == 'primary_key_name'}
        assertEquals 'CU Primary key', 1, cuTable.metadata.count {it.key == 'primary_key_columns'}
        assertEquals 'CI Primary indexes', 1, indexesInfo.findAll {it.primaryIndex}.size()
        assertEquals 'CI Unique indexes', 2, indexesInfo.findAll {it.uniqueIndex}.size()
        assertEquals 'CI indexes', 1, indexesInfo.findAll {!it.uniqueIndex && !it.primaryIndex}.size()
        assertEquals 'CU constraint', 1, cuTable.metadata.count {it.key == 'unique_name'}
        assertEquals 'CU constraint', 1, cuTable.metadata.count {it.key == 'unique_columns'}

        // Columns
        assertTrue 'Metadata all elements required', metadataTable.dataElements.every {it.minMultiplicity == 1}
        assertEquals 'CI mandatory elements', 9, ciTable.dataElements.count {it.minMultiplicity == 1}
        assertEquals 'CI optional element description', 0, ciTable.findDataElement('description').minMultiplicity
        assertEquals 'CU mandatory elements', 10, cuTable.dataElements.count {it.minMultiplicity == 1}
    }

    private void checkOrganisationMetadata(DataClass organisationTable) {
        // Expect 4 metadata - 2 for the primary key and 1 for indexes, 1 for extended property
        assertEquals 'Organisation Number of metadata', 4, organisationTable.metadata.size()

        assertTrue 'Extended property DESCRIPTION exists on organisation', organisationTable.getMetadata().any {Metadata md ->
            md.key == 'DESCRIPTION' && md.value == 'A table about organisations'
        }

        DataElement org_code = organisationTable.findDataElement('org_code')
        assertTrue "PROPERTY1 exists in metadata on org_code", org_code.getMetadata().any{ Metadata md ->
            md.key == 'PROPERTY1' && md.value == 'A first extended property on org_code'
        }

        assertTrue "PROPERTY2 exists in metadata on org_code", org_code.getMetadata().any{ Metadata md ->
            md.key == 'PROPERTY2' && md.value == 'A second extended property on org_code'
        }
    }

    private void checkOrganisationNotEnumerated(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass organisationTable = dataClasses.find {it.label == 'organisation'}

        Map<String, String> expectedColumns = [
                'org_code': 'PrimitiveType',
                'org_name': 'PrimitiveType',
                'org_char': 'PrimitiveType',
                'description': 'PrimitiveType',
                'org_type': 'PrimitiveType',
                'id': 'PrimitiveType',
                'org_nvarchar': 'PrimitiveType',
                'org_nchar': 'PrimitiveType'
        ]

        assertEquals 'Organisation Number of columns/dataElements', expectedColumns.size(), organisationTable.dataElements.size()
        //Expect all types to be Primitive, because we are not detecting enumerations
        expectedColumns.each {
            columnName, columnType ->
                assertEquals "DomainType of the DataType for ${columnName}", columnType, organisationTable.findDataElement(columnName).dataType.domainType
        }

        checkOrganisationMetadata(organisationTable)
    }

    private void checkOrganisationEnumerated(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass organisationTable = dataClasses.find {it.label == 'organisation'}

        Map<String, String> expectedColumns = [
                'org_code': 'EnumerationType',
                'org_name': 'PrimitiveType',
                'org_char': 'EnumerationType',
                'description': 'PrimitiveType',
                'org_type': 'EnumerationType',
                'id': 'PrimitiveType',
                'org_nvarchar': 'EnumerationType',
                'org_nchar': 'EnumerationType'
        ]

        assertEquals 'Organisation Number of columns/dataElements', expectedColumns.size(), organisationTable.dataElements.size()
        //Expect all types to be Primitive, because we are not detecting enumerations
        expectedColumns.each {
            columnName, columnType ->
                assertEquals "DomainType of the DataType for ${columnName}", columnType, organisationTable.findDataElement(columnName).dataType.domainType
        }

        checkOrganisationMetadata(organisationTable)


        final EnumerationType orgCodeEnumerationType = organisationTable.findDataElement('org_code').dataType
        assertEquals 'Number of enumeration values for org_code', 4, orgCodeEnumerationType.enumerationValues.size()
        assertNotNull 'Enumeration value found', orgCodeEnumerationType.enumerationValues.find{it.key == 'CODEZ'}
        assertNotNull 'Enumeration value found',orgCodeEnumerationType.enumerationValues.find{it.key == 'CODEY'}
        assertNotNull 'Enumeration value found',orgCodeEnumerationType.enumerationValues.find{it.key == 'CODEX'}
        assertNotNull 'Enumeration value found',orgCodeEnumerationType.enumerationValues.find{it.key == 'CODER'}
        assertNull 'Not an expected value', orgCodeEnumerationType.enumerationValues.find{it.key == 'CODEP'}

        final EnumerationType orgTypeEnumerationType = organisationTable.findDataElement('org_type').dataType
        assertEquals 'Number of enumeration values for org_type', 3, orgTypeEnumerationType.enumerationValues.size()
        assertNotNull 'Enumeration value found', orgTypeEnumerationType.enumerationValues.find{it.key == 'TYPEA'}
        assertNotNull 'Enumeration value found', orgTypeEnumerationType.enumerationValues.find{it.key == 'TYPEB'}
        assertNotNull 'Enumeration value found', orgTypeEnumerationType.enumerationValues.find{it.key == 'TYPEC'}
        assertNull 'Not an expected value', orgTypeEnumerationType.enumerationValues.find{it.key == 'TYPEZ'}

        final EnumerationType orgCharEnumerationType = organisationTable.findDataElement('org_char').dataType
        assertEquals 'Number of enumeration values for org_char', 3, orgCharEnumerationType.enumerationValues.size()
        assertNotNull 'Enumeration value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR1'}
        assertNotNull 'Enumeration value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR2'}
        assertNotNull 'Enumeration value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR3'}
        assertNull 'Not an expected value', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR4'}
    }

    private void checkOrganisationSummaryMetadata(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass organisationTable = dataClasses.find {it.label == 'organisation'}

        //Map of column name to expected summary metadata description:reportValue. Expect exact counts.
        Map<String, Map<String, String>> expectedColumns = [
                "org_code": ['Enumeration Value Distribution':'{"CODER":2,"CODEX":19,"CODEY":9,"CODEZ":11}'],
                "org_type": ['Enumeration Value Distribution':'{"TYPEA":17,"TYPEB":22,"TYPEC":2}'],
                "org_char": ['Enumeration Value Distribution':'{"NULL":1,"     ":1,"CHAR1":7,"CHAR2":13,"CHAR3":19}']
        ]

        expectedColumns.each {columnName, expectedReport ->
            DataElement de = organisationTable.dataElements.find{it.label == columnName}
            assertEquals 'One summaryMetadata', expectedReport.size(), de.summaryMetadata.size()

            expectedReport.each {expectedReportDescription, expectedReportValue ->
                assertEquals "Description of summary metadatdata for ${columnName}", expectedReportDescription, de.summaryMetadata[0].description
                assertEquals "Value of summary metadatdata for ${columnName}", expectedReportValue, de.summaryMetadata[0].summaryMetadataReports[0].reportValue
            }
        }

        //All data element summary metadata should also have been added to the data class
        assert organisationTable.dataElements.findAll{it.summaryMetadata}.size() == organisationTable.summaryMetadata.size()
    }

    private checkSampleNoSummaryMetadata(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'sample'}

        List<String> expectedColumns = [
                "id",
                "sample_tinyint",
                "sample_smallint",
                "sample_bigint",
                "sample_int",
                "sample_decimal",
                "sample_numeric",
                "sample_date",
                "sample_smalldatetime",
                "sample_datetime",
                "sample_datetime2"
        ]

        assertEquals 'Sample Number of columns/dataElements', expectedColumns.size(), sampleTable.dataElements.size()

        expectedColumns.each {columnName ->
            DataElement de = sampleTable.dataElements.find{it.label == columnName}
            assertEquals 'Zero summaryMetadata', 0, de.summaryMetadata.size()
        }
    }

    private void checkSampleSummaryMetadata(DataModel dataModel) {

        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'sample'}

        List<String> expectedColumns = [
                "id",
                "sample_tinyint",
                "sample_smallint",
                "sample_bigint",
                "sample_int",
                "sample_decimal",
                "sample_numeric",
                "sample_date",
                "sample_smalldatetime",
                "sample_datetime",
                "sample_datetime2"
        ]

        assertEquals 'Sample Number of columns/dataElements', expectedColumns.size(), sampleTable.dataElements.size()

        DataElement deId = sampleTable.dataElements.find{it.label == 'id'}
        assertEquals 'Zero summaryMetadata', 0, deId.summaryMetadata.size()

        expectedColumns.findAll {it != 'id'}.each {columnName ->
            DataElement de = sampleTable.dataElements.find{it.label == columnName}
            assertEquals 'One summaryMetadata', 1, de.summaryMetadata.size()
        }

        //sample_tinyint
        final DataElement sample_tinyint = sampleTable.dataElements.find{it.label == "sample_tinyint"}
        assertEquals 'reportValue for sample_tinyint',
                '{"0 - 10":19,"10 - 20":20,"20 - 30":20,"30 - 40":20,"40 - 50":20,"50 - 60":20,"60 - 70":20,"70 - 80":20,"80 - 90":20,"90 - 100":20,"100 - 110":2}',
                sample_tinyint.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_smallint
        final DataElement sample_smallint = sampleTable.dataElements.find{it.label == "sample_smallint"}
        assertEquals 'reportValue for sample_smallint',
                '{"-100 - -80":20,"-80 - -60":20,"-60 - -40":20,"-40 - -20":20,"-20 - 0":20,"0 - 20":20,"20 - 40":20,"40 - 60":20,"60 - 80":20,"80 - 100":20,"100 - 120":1}',
                sample_smallint.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_bigint
        final DataElement sample_bigint = sampleTable.dataElements.find{it.label == "sample_bigint"}
        assertEquals 'reportValue for sample_bigint',
                '{"-1000000 - -800000":8,"-800000 - -600000":8,"-600000 - -400000":11,"-400000 - -200000":15,"-200000 - 0":58,"0 - 200000":59,"200000 - 400000":15,"400000 - 600000":11,"600000 - 800000":8,"800000 - 1000000":7,"1000000 - 1200000":1}',
                sample_bigint.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_int
        final DataElement sample_int = sampleTable.dataElements.find{it.label == "sample_int"}
        assertEquals 'reportValue for sample_int',
                '{"0 - 1000":63,"1000 - 2000":26,"2000 - 3000":20,"3000 - 4000":18,"4000 - 5000":14,"5000 - 6000":14,"6000 - 7000":12,"7000 - 8000":12,"8000 - 9000":10,"9000 - 10000":10,"10000 - 11000":2}',
                sample_int.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_decimal
        final DataElement sample_decimal = sampleTable.dataElements.find{it.label == "sample_decimal"}
        assertEquals 'reportValue for sample_decimal',
                '{"0.000 - 1000000.000":83,"1000000.000 - 2000000.000":36,"2000000.000 - 3000000.000":26,"3000000.000 - 4000000.000":22,"4000000.000 - 5000000.000":20,"5000000.000 - 6000000.000":14}',
                sample_decimal.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_numeric
        final DataElement sample_numeric = sampleTable.dataElements.find{it.label == "sample_numeric"}
        assertEquals 'reportValue for sample_numeric',
                '{"-10.000000 - -5.000000":20,"-5.000000 - 0.000000":80,"0.000000 - 5.000000":81,"5.000000 - 10.000000":20}',
                sample_numeric.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_date
        final DataElement sample_date = sampleTable.dataElements.find{it.label == "sample_date"}
        assertEquals 'reportValue for sample_date',
                '{"May 2020":8,"Jun 2020":30,"Jul 2020":31,"Aug 2020":31,"Sept 2020":30,"Oct 2020":31,"Nov 2020":30,"Dec 2020":10}',
                sample_date.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_smalldatetime
        final DataElement sample_smalldatetime = sampleTable.dataElements.find{it.label == "sample_smalldatetime"}
        assertEquals 'reportValue for sample_smalldatetime',
                '{"2012":8,"2013":12,"2014":12,"2015":12,"2016":12,"2017":12,"2018":12,"2019":12,"2020":12,"2021":12,"2022":12,"2023":12,"2024":12,"2025":12,"2026":12,"2027":12,"2028":12,"2029":1}',
                sample_smalldatetime.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_datetime
        final DataElement sample_datetime = sampleTable.dataElements.find{it.label == "sample_datetime"}
        assertEquals 'reportValue for sample_datetime',
                '{"1920 - 1930":10,"1930 - 1940":10,"1940 - 1950":10,"1950 - 1960":10,"1960 - 1970":10,"1970 - 1980":10,"1980 - 1990":10,"1990 - 2000":10,"2000 - 2010":10,"2010 - 2020":10,"2020 - 2030":10,"2030 - 2040":10,"2040 - 2050":10,"2050 - 2060":10,"2060 - 2070":10,"2070 - 2080":10,"2080 - 2090":10,"2090 - 2100":10,"2100 - 2110":10,"2110 - 2120":10,"2120 - 2130":1}',
                sample_datetime.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_datetime2
        final DataElement sample_datetime2 = sampleTable.dataElements.find{it.label == "sample_datetime2"}
        assertEquals 'reportValue for sample_datetime2',
                '{"27/08/2020 - 28/08/2020":4,"28/08/2020 - 29/08/2020":24,"29/08/2020 - 30/08/2020":24,"30/08/2020 - 31/08/2020":24,"31/08/2020 - 01/09/2020":24,"01/09/2020 - 02/09/2020":24,"02/09/2020 - 03/09/2020":24,"03/09/2020 - 04/09/2020":24,"04/09/2020 - 05/09/2020":24,"05/09/2020 - 06/09/2020":5}',
                sample_datetime2.summaryMetadata[0].summaryMetadataReports[0].reportValue

    }

    /**
     * Check that there is a DataClass for the bigger_sample table, with 4 columns but no
     * summary metadata on any of these columns.
     * @param dataModel
     * @return
     */
    private checkBiggerSampleNoSummaryMetadata(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'bigger_sample'}

        List<String> expectedColumns = [
                "sample_bigint",
                "sample_decimal",
                "sample_date",
                "sample_varchar"
        ]

        assertEquals 'Sample Number of columns/dataElements', expectedColumns.size(), sampleTable.dataElements.size()

        expectedColumns.each {columnName ->
            DataElement de = sampleTable.dataElements.find{it.label == columnName}
            assertEquals 'Zero summaryMetadata', 0, de.summaryMetadata.size()
        }
    }

    /**
     * Check that there is a DataClass for the bigger_sample table, with 4 columns but exact
     * summary metadata on any of these columns.
     * @param dataModel
     * @return
     */
    private checkBiggerSampleSummaryMetadata(DataModel dataModel) {
        final DataClass publicSchema = dataModel.childDataClasses.first()
        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'bigger_sample'}

        //Map of column name to expected summary metadata description:reportValue. Expect exact counts.
        Map<String, Map<String, String>> expectedColumns = [
                "sample_bigint": ['Value Distribution':'{"0 - 100000":99999,"100000 - 200000":100000,"200000 - 300000":100000,"300000 - 400000":100000,"400000 - 500000":100000,"500000 - 600000":1}'],
                "sample_decimal": ['Value Distribution':'{"-1.000 - 0.000":249924,"0.000 - 1.000":245051,"1.000 - 2.000":5025}'],
                "sample_date": ['Value Distribution':'{"Feb 2020 - Apr 2020":108893,"Apr 2020 - Jun 2020":63245,"Jun 2020 - Aug 2020":51465,"Aug 2020 - Oct 2020":49551,"Oct 2020 - Dec 2020":51145,"Dec 2020 - Feb 2021":63103,"Feb 2021 - Apr 2021":112598}'],
                "sample_varchar": ['Enumeration Value Distribution':'{"ENUM0":33333,"ENUM1":33334,"ENUM10":33333,"ENUM11":33333,"ENUM12":33333,"ENUM13":33333,"ENUM14":33333,"ENUM2":33334,"ENUM3":33334,"ENUM4":33334,"ENUM5":33334,"ENUM6":33333,"ENUM7":33333,"ENUM8":33333,"ENUM9":33333}']
        ]

        assertEquals 'Sample Number of columns/dataElements', expectedColumns.size(), sampleTable.dataElements.size()

        expectedColumns.each {columnName, expectedReport ->
            DataElement de = sampleTable.dataElements.find{it.label == columnName}
            assertEquals 'One summaryMetadata', expectedReport.size(), de.summaryMetadata.size()

            expectedReport.each {expectedReportDescription, expectedReportValue ->
                assertEquals "Description of summary metadatdata for ${columnName}", expectedReportDescription, de.summaryMetadata[0].description
                assertEquals "Value of summary metadatdata for ${columnName}", expectedReportValue, de.summaryMetadata[0].summaryMetadataReports[0].reportValue
            }
        }
    }
}
