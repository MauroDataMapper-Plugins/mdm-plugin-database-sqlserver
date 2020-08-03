package ox.softeng.metadatacatalogue.plugins.database.sqlserver

import ox.softeng.metadatacatalogue.core.catalogue.linkable.component.datatype.DataType
import ox.softeng.metadatacatalogue.core.catalogue.linkable.component.datatype.PrimitiveType
import ox.softeng.metadatacatalogue.core.traits.spi.datatype.DefaultDataTypeProvider

/**
 * @since 19/04/2018
 */
class SqlServerDefaultDataTypeProvider implements DefaultDataTypeProvider {
    @Override
    List<DataType> getDefaultListOfDataTypes() {
        [
            new PrimitiveType(label: 'char(n)', description: 'Fixed width character string\n8,000 characters'),
            new PrimitiveType(label: 'varchar(n)', description: 'Variable width character string\n8,000 characters'),
            new PrimitiveType(label: 'varchar(max)', description: 'Variable width character string\n1,073,741,824 characters'),
            new PrimitiveType(label: 'text', description: 'Variable width character string\n2GB of text data'),
            new PrimitiveType(label: 'nchar', description: 'Fixed width Unicode string\n4,000 characters'),
            new PrimitiveType(label: 'nvarchar', description: 'Variable width Unicode string\n4,000 characters'),
            new PrimitiveType(label: 'nvarchar(max)', description: 'Variable width Unicode string\n536,870,912 characters'),
            new PrimitiveType(label: 'ntext', description: 'Variable width Unicode string\n2GB of text data'),
            new PrimitiveType(label: 'binary(n)', description: 'Fixed width binary string\n8,000 bytes'),
            new PrimitiveType(label: 'varbinary', description: 'Variable width binary string\n8,000 bytes'),
            new PrimitiveType(label: 'varbinary(max)', description: 'Variable width binary string\n2GB'),
            new PrimitiveType(label: 'image', description: 'Variable width binary string\n2GB'),
            new PrimitiveType(label: 'bit', description: 'Integer that can be 0, 1, or NULL'),
            new PrimitiveType(label: 'tinyint', description: 'Allows whole numbers from 0 to 255'),
            new PrimitiveType(label: 'smallint', description: 'Allows whole numbers between -32,768 and 32,767'),
            new PrimitiveType(label: 'int', description: 'Allows whole numbers between -2,147,483,648 and 2,147,483,647'),
            new PrimitiveType(label: 'bigint', description: 'Allows whole numbers between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807'),
            new PrimitiveType(label: 'decimal(p,s)', description: 'Fixed precision and scale numbers.\n' +
                                                                  'Allows numbers from -10^38 +1 to 10^38 –1.\n' +
                                                                  'The p parameter indicates the maximum total number of digits that can be stored ' +
                                                                  '(both to the left and to the right of ' +
                                                                  'the decimal point). p must be a value from 1 to 38. Default is 18.\n' +
                                                                  'The s parameter indicates the maximum number of digits stored to the right of ' +
                                                                  'the decimal point. s must be a value ' +
                                                                  'from 0 to p. Default value is 0'),
            new PrimitiveType(label: 'numeric(p,s)', description: 'Fixed precision and scale numbers.\n' +
                                                                  'Allows numbers from -10^38 +1 to 10^38 –1.\n' +
                                                                  'The p parameter indicates the maximum total number of digits that can be stored ' +
                                                                  '(both to the left and to the right of ' +
                                                                  'the decimal point). p must be a value from 1 to 38. Default is 18.\n' +
                                                                  'The s parameter indicates the maximum number of digits stored to the right of ' +
                                                                  'the decimal point. s must be a value ' +
                                                                  'from 0 to p. Default value is 0'),
            new PrimitiveType(label: 'smallmoney', description: 'Monetary data from -214,748.3648 to 214,748.3647'),
            new PrimitiveType(label: 'money', description: 'Monetary data from -922,337,203,685,477.5808 to 922,337,203,685,477.5807'),
            new PrimitiveType(label: 'float(n)', description: 'Floating precision number data from -1.79E + 308 to 1.79E + 308.\n' +
                                                              'The n parameter indicates whether the field should hold 4 or 8 bytes. float(24) ' +
                                                              'holds a 4-byte field and float(53) holds an 8-byte ' +
                                                              'field. Default value of n is 53.'),
            new PrimitiveType(label: 'real', description: 'Floating precision number data from -3.40E + 38 to 3.40E + 38'),
            new PrimitiveType(label: 'datetime', description: 'From January 1, 1753 to December 31, 9999 with an accuracy of 3.33 milliseconds'),
            new PrimitiveType(label: 'datetime2', description: 'From January 1, 0001 to December 31, 9999 with an accuracy of 100 nanoseconds'),
            new PrimitiveType(label: 'smalldatetime', description: 'From January 1, 1900 to June 6, 2079 with an accuracy of 1 minute'),
            new PrimitiveType(label: 'date', description: 'Store a date only. From January 1, 0001 to December 31, 9999'),
            new PrimitiveType(label: 'time', description: 'Store a time only to an accuracy of 100 nanoseconds'),
            new PrimitiveType(label: 'datetimeoffset', description: 'The same as datetime2 with the addition of a time zone offset'),
            new PrimitiveType(label: 'timestamp',
                              description: 'Stores a unique number that gets updated every time a row gets created or modified. The timestamp value' +
                                           ' is based upon an internal clock and does not correspond to real time. Each table may have only one ' +
                                           'timestamp variable'),
            new PrimitiveType(label: 'sql_variant',
                              description: 'Stores up to 8,000 bytes of data of various data types, except text, ntext, and timestamp'),
            new PrimitiveType(label: 'uniqueidentifier', description: 'Stores a globally unique identifier (GUID)'),
            new PrimitiveType(label: 'xml', description: 'Stores XML formatted data. Maximum 2GB'),
            new PrimitiveType(label: 'cursor', description: 'Stores a reference to a cursor used for database operations'),
            new PrimitiveType(label: 'table', description: 'Stores a result-set for later processing'),

        ]
    }

    @Override
    String getDisplayName() {
        'Transact-SQL / MSSQL DataTypes'
    }
}
