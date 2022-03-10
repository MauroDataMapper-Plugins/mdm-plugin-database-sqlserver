package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.calculation

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.CalculationStrategy

/**
 * @since 08/03/2022
 */
class SqlServerCalculationStrategy extends CalculationStrategy{
    SqlServerCalculationStrategy(DatabaseDataModelImporterProviderServiceParameters parameters) {
        super(parameters)
    }

    @Override
    boolean isColumnPossibleEnumeration(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ['char', 'varchar', 'nchar', 'nvarchar'].contains(dataType.label)
    }

    @Override
    boolean isColumnForDateSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ['date', 'smalldatetime', 'datetime', 'datetime2'].contains(dataType.label)
    }

    @Override
    boolean isColumnForDecimalSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ['decimal', 'numeric'].contains(dataType.label)
    }

    @Override
    boolean isColumnForIntegerSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ['tinyint', 'smallint', 'int'].contains(dataType.label)
    }

    @Override
    boolean isColumnForLongSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ['bigint'].contains(dataType.label)
    }
}
