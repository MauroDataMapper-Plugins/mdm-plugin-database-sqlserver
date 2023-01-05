/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
        [
            [
                label      : 'bigint',
                description: '''Integer numbers in the range from -2^63 (-9,223,372,036,854,775,808) to 2^63-1 (9,223,372,036,854,775,807).

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql).'''
            ],
            [
                label      : 'binary',
                description: '''Fixed-length binary data with a specified length of bytes. Maximum length is 8000 bytes.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/binary-and-varbinary-transact-sql)'''
            ],
            [
                label      : 'binary[n]',
                description: '''Fixed-length binary data with a specified length of bytes. Maximum length is 8000 bytes.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/binary-and-varbinary-transact-sql)'''
            ],
            [
                label      : 'bit',
                description: '''An integer data type that can take a value of 1, 0, or NULL.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/numeric-types)'''
            ],
            [
                label      : 'char',
                description: '''Fixed-size string up to 8,000 characters.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'char[n]',
                description: '''Fixed-size string up to 8,000 characters.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'cursor',
                description: '''Stores a reference to a cursor used for database operations
More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/cursor-transact-sql)'''
            ],
            [
                label      : 'date',
                description: '''Defines a date in the range from January 1, 0001 to December 31, 9999

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'datetime',
                description: '''Defines a date combined with a time of day in the range from January 1, 1753 to December 31, 9999 with an accuracy of 3.33 milliseconds.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'datetime2',
                description: '''Defines a date combined with a time of day in the range from January 1, 0001 to December 31, 9999 with an accuracy of 100 nanoseconds

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'datetimeoffset',
                description: '''The same as datetime2 with the addition of a time zone offset

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'decimal',
                description: '''Fixed precision and scale numbers.
Data range is from -10^38 +1 to 10^38 -1, depending on the precision and scale numbers.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/numeric-types)'''
            ],
            [
                label      : 'decimal[p,s]',
                description: '''Fixed precision and scale numbers.
Data range is from -10^38 +1 to 10^38 -1, depending on the precision and scale numbers.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/numeric-types)'''
            ],
            [
                label      : 'float[n]',
                description: '''Floating precision number data from -1.79E + 308 to 1.79E + 308.
The n parameter indicates whether the field should hold 4 or 8 bytes.
float[24] holds a 4-byte field and float[53] holds an 8-byte field.
Default value of n is 53.

More Info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql)'''
            ],
            [
                label      : 'image',
                description: '''Variable width binary string up to 2GB
 
More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/ntext-text-and-image-transact-sql)'''
            ],
            [
                label      : 'int',
                description: '''Integer numbers in the range from -2^31 (-2,147,483,648) to 2^31-1 (2,147,483,647).

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql)'''
            ],
            [
                label      : 'money',
                description: '''Monetary data accurate to a ten-thousandth of the monetary units that they represent.
Data range is from -922,337,203,685,477.5808 to 922,337,203,685,477.5807.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/money-and-smallmoney-transact-sql)'''
            ],
            [
                label      : 'nchar',
                description: '''Fixed-size string data with the maxim length of 4,000 bytes.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql)'''
            ],
            [
                label      : 'ntext',
                description: '''Variable-length Unicode data with a maximum string length of 2^30 - 1 (1,073,741,823) bytes.
Maximum of 2GB.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/ntext-text-and-image-transact-sql)'''
            ],
            [
                label      : 'numeric',
                description: '''Fixed precision and scale numbers.
Data range is from -10^38 +1 to 10^38 -1, depending on the precision and scale numbers.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql)'''
            ],
            [
                label      : 'numeric[p,s]',
                description: '''Fixed precision and scale numbers.
Data range is from -10^38 +1 to 10^38 -1, depending on the precision and scale numbers.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql)'''
            ],
            [
                label      : 'nvarchar',
                description: '''Variable-size string data with the maxim size of 4,000 bytes.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql)'''
            ],
            [
                label      : 'nvarchar[max]',
                description: '''Fixed-size string data when the sizes exceeds 4,000 bytes.
Maximum of 2GB.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql)'''
            ],
            [
                label      : 'real',
                description: '''Floating precision numbers (same as float(24)) in the range from -3.40E +38 to 3.40E +38.

[More Info: Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql)'''
            ],
            [
                label      : 'smalldatetime',
                description: '''Defines a date in the range from January 1, 1900 to June 6, 2079 with an accuracy of 1 minute

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'smallint',
                description: '''Allows whole numbers between in the range from -2^15 (-32,768) to 2^15-1 (32,767)-32,768 and 32,767.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql)'''
            ],
            [
                label      : 'smallmoney',
                description: '''Monetary data accurate to a ten-thousandth of the monetary units that they represent.
Data range is from -214,748.3648 to 214,748.3647.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/money-and-smallmoney-transact-sql)'''
            ],
            [
                label      : 'sql_variant',
                description: '''Up to 8,016 bytes of data of various data types.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/sql-variant-transact-sql)'''
            ],
            [
                label      : 'table',
                description: '''Stores a result-set for later processing
More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/table-transact-sql)'''
            ],
            [
                label      : 'text',
                description: '''Variable-length non-Unicode data with a maximum string length of 2^31 - 1 (2,147,483,647) bytes.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/ntext-text-and-image-transact-sql)'''
            ],
            [
                label      : 'time',
                description: '''Store a time only to an accuracy of 100 nanoseconds

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/date-and-time-types)'''
            ],
            [
                label      : 'timestamp',
                description: '''Stores a unique number that gets updated every time a row gets created or modified.
The timestamp value is based upon an internal clock and does not correspond to real time.
Each table may have only one timestamp variable.'''
            ],
            [
                label      : 'tinyint',
                description: '''Integer numbers in the range from 0 to 255.

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql)'''
            ],
            [
                label      : 'uniqueidentifier',
                description: '''Defines a globally 16-byte unique identifier [GUID].

More info: [Microsoft T-SQL documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/uniqueidentifier-transact-sql)'''
            ],
            [
                label      : 'varbinary',
                description: '''Variable-length binary data with a specified length of n bytes. Maximum value for n is 8000.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'varbinary[max]',
                description: '''Variable-length binary data when the size may exceed 8000 bytes.
Maximum of 2GB.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'varchar',
                description: '''Variable-size string data up to 8000 characters.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'varchar[max]',
                description: '''Variable-size string data when the size may exceed 8000 bytes.
Maximum of 2GB.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'varchar[n]',
                description: '''Variable-size string data up to 8000 characters.

More Info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/data-types/string-and-binary-types)'''
            ],
            [
                label      : 'xml',
                description: '''Data type that stores XML data.
Maximum of 2GB.

More info: [Microsoft T-SQL Documentation](https://docs.microsoft.com/en-us/sql/t-sql/xml/xml-transact-sql)'''
            ],
        ].collect {Map<String, String> properties -> new DefaultDataType(new PrimitiveType(properties))}
    }
}
