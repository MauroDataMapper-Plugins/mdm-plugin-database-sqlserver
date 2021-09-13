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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
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
        assertEquals 'Number of columntypes/datatypes', 18, dataModel.dataTypes?.size()
        assertEquals 'Number of primitive types', 16, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 0, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of char datatypes', 1, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType' && it.label == 'char'}.size()

        /**
         * Expect data classes for:
         * dbo (child of the data model)
         * catalogue_item (child of dbo)
         * catalogue_user (child of dbo)
         * metadata (child of dbo)
         * organisation (child of dbo)
         * sample (child of dbo)
         */
        assertEquals 'Number of tables/dataclasses', 6, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 5, publicSchema.dataClasses?.size()

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
        assertEquals 'Number of columntypes/datatypes', 20, dataModel.dataTypes?.size()
        assertEquals 'Number of primitive types', 15, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType'}.size()
        assertEquals 'Number of reference types', 2, dataModel.dataTypes.findAll {it.domainType == 'ReferenceType'}.size()
        assertEquals 'Number of enumeration types', 3, dataModel.dataTypes.findAll {it.domainType == 'EnumerationType'}.size()
        assertEquals 'Number of char datatypes', 0, dataModel.dataTypes.findAll {it.domainType == 'PrimitiveType' && it.label == 'char'}.size()
        assertEquals 'Number of tables/dataclasses', 6, dataModel.dataClasses?.size()
        assertEquals 'Number of child tables/dataclasses', 1, dataModel.childDataClasses?.size()

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 5, publicSchema.dataClasses?.size()

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
    void 'testImportSimpleDatabaseWithSummaryMetadata'() {
        final DataModel dataModel = importDataModelAndRetrieveFromDatabase(
                createDatabaseImportParameters(databaseHost, databasePort).tap {
                    databaseNames = 'metadata_simple';
                    detectEnumerations = true;
                    maxEnumerations = 20;
                    calculateSummaryMetadata = true;
                })

        final DataClass publicSchema = dataModel.childDataClasses.first()
        assertEquals 'Number of child tables/dataclasses', 5, publicSchema.dataClasses?.size()

        final Set<DataClass> dataClasses = publicSchema.dataClasses
        final DataClass sampleTable = dataClasses.find {it.label == 'sample'}

        assertEquals 'Sample Number of columns/dataElements', 11, sampleTable.dataElements.size()

        final DataElement id = sampleTable.dataElements.find{it.label == "id"}
        //Expect id to have contiguous values from 1 to 201
        assertEquals 'reportValue for id',
                '{"0 - 20":19,"20 - 40":20,"40 - 60":20,"60 - 80":20,"80 - 100":20,"100 - 120":20,"120 - 140":20,"140 - 160":20,"160 - 180":20,"180 - 200":20,"200 - 220":2}',
                id.summaryMetadata[0].summaryMetadataReports[0].reportValue

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
                '{"-5.000000 - 0.000000":80,"0.000000 - 5.000000":81,"5.000000 - 10.000000":20}',
                sample_numeric.summaryMetadata[0].summaryMetadataReports[0].reportValue

        //sample_date
        final DataElement sample_date = sampleTable.dataElements.find{it.label == "sample_date"}
        assertEquals 'reportValue for sample_date',
                '{"May 2020":8,"Jun 2020":30,"Jul 2020":31,"Aug 2020":31,"Sep 2020":30,"Oct 2020":31,"Nov 2020":30,"Dec 2020":10}',
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
}
