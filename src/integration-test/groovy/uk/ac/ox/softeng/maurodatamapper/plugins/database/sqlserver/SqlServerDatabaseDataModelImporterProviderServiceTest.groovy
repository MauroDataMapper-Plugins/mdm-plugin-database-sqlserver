/*
 * Copyright 2020-2021 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.plugins.testing.utils.BaseDatabasePluginTest

import groovy.json.JsonSlurper
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

// @CompileStatic
class SqlServerDatabaseDataModelImporterProviderServiceTest
    extends BaseDatabasePluginTest<SqlServerDatabaseDataModelImporterProviderServiceParameters, SqlServerDatabaseDataModelImporterProviderService> {

    @Override
    String getDatabasePortPropertyName() {
        'unknown'
    }

    @Override
    int getDefaultDatabasePort() {
        1433
    }

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

    @Test
    @Ignore('no credentials')
    void testConnectionToOuh() {
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
            folderId = getTestFolder().getId()
        }

        DataModel lims = importDataModelAndRetrieveFromDatabase(params)

        assert lims
    }

    @Test
    void testImportSimpleDatabase() {
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
            createDatabaseImportParameters(databaseHost, databasePort).tap {databaseNames = 'metadata_simple'})
        assertEquals 'Database/Model name', 'metadata_simple', dataModel.label
        assertEquals 'Number of columntypes/datatypes', 11, dataModel.dataTypes?.size()
        assertEquals 'Number of primitive types', 9, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 0, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of char datatypes', 1, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType' && it.label == 'char'}.size()
        assertEquals 'Number of tables/dataclasses', 5, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 4, publicSchema.dataClasses?.size()

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

        final DataClass organisationTable = dataClasses.find {it.label == 'organisation'}
        assertEquals 'Organisation Number of columns/dataElements', 6, organisationTable.dataElements.size()
        // Expect 3 metadata - 2 for the primary key and 1 for indexes
        assertEquals 'Organisation Number of metadata', 3, organisationTable.metadata.size()
        //Expect all types to be Primitive, because we are not detecting enumerations
        assertEquals 'DomainType of the DataType for org_code', 'PrimitiveType', organisationTable.findDataElement('org_code').dataType.domainType
        assertEquals 'DomainType of the DataType for org_name', 'PrimitiveType', organisationTable.findDataElement('org_name').dataType.domainType
        assertEquals 'DomainType of the DataType for org_char', 'PrimitiveType', organisationTable.findDataElement('org_char').dataType.domainType
        assertEquals 'DomainType of the DataType for description', 'PrimitiveType', organisationTable.findDataElement('description').dataType.domainType
        assertEquals 'DomainType of the DataType for org_type', 'PrimitiveType', organisationTable.findDataElement('org_type').dataType.domainType
        assertEquals 'DomainType of the DataType for id', 'PrimitiveType', organisationTable.findDataElement('id').dataType.domainType
    }

    @Test
    void testImportSimpleDatabaseWithEnumerations() {
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
                createDatabaseImportParameters(databaseHost, databasePort).tap {
                    databaseNames = 'metadata_simple';
                    detectEnumerations = true;
                    maxEnumerations = 20})
        assertEquals 'Database/Model name', 'metadata_simple', dataModel.label
        assertEquals 'Number of columntypes/datatypes', 13, dataModel.dataTypes?.size()
        assertEquals 'Number of primitive types', 8, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 3, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of char datatypes', 0, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType' && it.label == 'char'}.size()
        assertEquals 'Number of tables/dataclasses', 5, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 4, publicSchema.dataClasses?.size()

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

        final DataClass organisationTable = dataClasses.find {it.label == 'organisation'}
        assertEquals 'Organisation Number of columns/dataElements', 6, organisationTable.dataElements.size()
        // Expect 3 metadata - 2 for the primary key and 1 for indexes
        assertEquals 'Organisation Number of metadata', 3, organisationTable.metadata.size()
        // Expect org_code, org_char and org_type to have been detected as EnumerationType
        assertEquals 'DomainType of the DataType for org_code', 'EnumerationType', organisationTable.findDataElement('org_code').dataType.domainType
        assertEquals 'DomainType of the DataType for org_name', 'PrimitiveType', organisationTable.findDataElement('org_name').dataType.domainType
        assertEquals 'DomainType of the DataType for org_char', 'EnumerationType', organisationTable.findDataElement('org_char').dataType.domainType
        assertEquals 'DomainType of the DataType for description', 'PrimitiveType', organisationTable.findDataElement('description').dataType.domainType
        assertEquals 'DomainType of the DataType for org_type', 'EnumerationType', organisationTable.findDataElement('org_type').dataType.domainType
        assertEquals 'DomainType of the DataType for id', 'PrimitiveType', organisationTable.findDataElement('id').dataType.domainType

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
        assertNotNull 'Enumeration   value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR1'}
        assertNotNull 'Enumeration value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR2'}
        assertNotNull 'Enumeration value found', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR3'}
        assertNull 'Not an expected value', orgCharEnumerationType.enumerationValues.find{it.key == 'CHAR4'}
    }

    @Test
    void 'S01 testImportSimpleDatabaseWithSummaryMetadata'() {
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
                createDatabaseImportParameters(databaseHost, databasePort).tap {
                    databaseNames = 'metadata_simple';
                    detectEnumerations = true;
                    maxEnumerations = 20;
                    calculateSummaryMetadata = true;
                })

    }
}
