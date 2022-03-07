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


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DefaultDataType

// @CompileStatic
class SqlServerDataTypeProviderService implements DefaultDataTypeProvider {

    @Override
    String getDisplayName() {
        'Transact-SQL / MSSQL DataTypes'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    List<DefaultDataType> getDefaultListOfDataTypes() {
        final String decimalDescription = '''
            Fixed precision and scale numbers.
            Allows numbers from -10^38 +1 to 10^38 –1.
            The p parameter indicates the maximum total number of digits that can be stored [both to the left and right of the decimal point].
            p must be a value from 1 to 38. Default is 18.
            The s parameter indicates the maximum number of digits stored to the right of the decimal point.
            s must be a value from 0 to p. Default value is 0.
        '''.stripIndent()
        final String numericDescription = '''
            Fixed precision and scale numbers.
            Allows numbers from -10^38 +1 to 10^38 –1.
            The p parameter indicates the maximum total number of digits that can be stored [both to the left and right of the decimal point].
            p must be a value from 1 to 38. Default is 18.
            The s parameter indicates the maximum number of digits stored to the right of the decimal point.
            s must be a value from 0 to p. Default value is 0.
        '''.stripIndent()
        final String floatDescription = '''
            Floating precision number data from -1.79E + 308 to 1.79E + 308.
            The n parameter indicates whether the field should hold 4 or 8 bytes.
            float[24] holds a 4-byte field and float[53] holds an 8-byte field.
            Default value of n is 53.
        '''.stripIndent()
        final String timestampDescription = '''
            Stores a unique number that gets updated every time a row gets created or modified.
            The timestamp value is based upon an internal clock and does not correspond to real time.
            Each table may have only one timestamp variable.
        '''.stripIndent()

        [[label: 'char', description: 'Fixed width character string\n8,000 characters'],
         [label: 'char[n]', description: 'Fixed width character string\n8,000 characters'],
         [label: 'varchar', description: 'Variable width character string\n8,000 characters'],
         [label: 'varchar[n]', description: 'Variable width character string\n8,000 characters'],
         [label: 'varchar[max]', description: 'Variable width character string\n1,073,741,824 characters'],
         [label: 'text', description: 'Variable width character string\n2GB of text data'],
         [label: 'nchar', description: 'Fixed width Unicode string\n4,000 characters'],
         [label: 'nvarchar', description: 'Variable width Unicode string\n4,000 characters'],
         [label: 'nvarchar[max]', description: 'Variable width Unicode string\n536,870,912 characters'],
         [label: 'ntext', description: 'Variable width Unicode string\n2GB of text data'],
         [label: 'binary', description: 'Fixed width binary string\n8,000 bytes'],
         [label: 'binary[n]', description: 'Fixed width binary string\n8,000 bytes'],
         [label: 'varbinary', description: 'Variable width binary string\n8,000 bytes'],
         [label: 'varbinary[max]', description: 'Variable width binary string\n2GB'],
         [label: 'image', description: 'Variable width binary string\n2GB'],
         [label: 'bit', description: 'Integer that can be 0, 1, or NULL'],
         [label: 'tinyint', description: 'Allows whole numbers from 0 to 255'],
         [label: 'smallint', description: 'Allows whole numbers between -32,768 and 32,767'],
         [label: 'int', description: 'Allows whole numbers between -2,147,483,648 and 2,147,483,647'],
         [label: 'bigint', description: 'Allows whole numbers between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807'],
         [label: 'decimal', description: decimalDescription],
         [label: 'decimal[p,s]', description: decimalDescription],
         [label: 'numeric', description: numericDescription],
         [label: 'numeric[p,s]', description: numericDescription],
         [label: 'smallmoney', description: 'Monetary data from -214,748.3648 to 214,748.3647'],
         [label: 'money', description: 'Monetary data from -922,337,203,685,477.5808 to 922,337,203,685,477.5807'],
         [label: 'float[n]', description: floatDescription],
         [label: 'real', description: 'Floating precision number data from -3.40E + 38 to 3.40E + 38'],
         [label: 'datetime', description: 'From January 1, 1753 to December 31, 9999 with an accuracy of 3.33 milliseconds'],
         [label: 'datetime2', description: 'From January 1, 0001 to December 31, 9999 with an accuracy of 100 nanoseconds'],
         [label: 'smalldatetime', description: 'From January 1, 1900 to June 6, 2079 with an accuracy of 1 minute'],
         [label: 'date', description: 'Store a date only. From January 1, 0001 to December 31, 9999'],
         [label: 'time', description: 'Store a time only to an accuracy of 100 nanoseconds'],
         [label: 'datetimeoffset', description: 'The same as datetime2 with the addition of a time zone offset'],
         [label: 'timestamp', description: timestampDescription],
         [label: 'sql_variant', description: 'Stores up to 8,000 bytes of data of various data types, except text, ntext, and timestamp'],
         [label: 'uniqueidentifier', description: 'Stores a globally unique identifier [GUID]'],
         [label: 'xml', description: 'Stores XML formatted data. Maximum 2GB'],
         [label: 'cursor', description: 'Stores a reference to a cursor used for database operations'],
         [label: 'table', description: 'Stores a result-set for later processing'],
        ].collect {Map<String, String> properties -> new DefaultDataType(new PrimitiveType(properties))}
    }
}
