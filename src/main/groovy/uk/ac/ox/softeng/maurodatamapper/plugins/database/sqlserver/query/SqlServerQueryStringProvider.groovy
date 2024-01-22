/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.query

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.summarymetadata.AbstractIntervalHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.CalculationStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.SamplingStrategy
import uk.ac.ox.softeng.maurodatamapper.plugins.database.query.QueryStringProvider

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * @since 11/03/2022
 */
class SqlServerQueryStringProvider extends QueryStringProvider{

    @Override
    String getIndexInformationQueryString() {
        '''
        SELECT OBJECT_NAME(i.object_id)           AS table_name,
               i.name                             AS index_name,
               i.is_unique                        AS unique_index,
               i.is_primary_key                   AS primary_index,
               CAST(
                 (CASE i.type_desc
                    WHEN 'CLUSTERED' THEN 1
                    ELSE 0 END)
                 AS BIT)                          AS is_clustered,
               CAST(
                 (SUBSTRING(
                   (SELECT ', ' + COL_NAME(ic2.object_id, ic2.column_id) AS [text()]
                    FROM sys.index_columns ic2
                    WHERE i.object_id = ic2.object_id
                      AND i.index_id = ic2.index_id
                    ORDER BY ic2.index_column_id
                       FOR XML PATH ('')), 2, 1000)) AS VARCHAR(1000)) AS column_names
        FROM sys.indexes AS i
               INNER JOIN sys.index_columns AS ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
               LEFT JOIN  sys.objects as o ON i.object_id = o.object_id
        WHERE OBJECT_SCHEMA_NAME(i.object_id) = ?
              AND i.name IS NOT NULL
        GROUP BY i.object_id, i.index_id, i.name, i.is_unique, i.is_primary_key, i.type_desc;
        '''.stripIndent()
    }

    @Override
    String getForeignKeyInformationQueryString() {
        '''
        SELECT
          f.name                                                     AS constraint_name,
          OBJECT_SCHEMA_NAME(f.parent_object_id)                     AS schema_name,
          OBJECT_NAME(f.parent_object_id)                            AS table_name,
          COL_NAME(fc.parent_object_id, fc.parent_column_id)         AS column_name,
          OBJECT_SCHEMA_NAME(f.referenced_object_id)                 AS reference_schema_name,
          OBJECT_NAME(f.referenced_object_id)                        AS reference_table_name,
          COL_NAME(fc.referenced_object_id, fc.referenced_column_id) AS reference_column_name
        FROM sys.foreign_keys AS f
          INNER JOIN sys.foreign_key_columns AS fc
            ON f.OBJECT_ID = fc.constraint_object_id
        INNER JOIN sys.schemas AS s ON f.schema_id = s.schema_id
        WHERE s.name = ?;
        '''.stripIndent()
    }

    @Override
    String getDatabaseStructureQueryString() {
        'SELECT * FROM INFORMATION_SCHEMA.COLUMNS;'
    }

    /**
     * SQL Server square bracket escaping of identifiers
     */
    @Override
    String escapeIdentifier(String identifier) {
        "[${identifier}]"
    }

    /**
     * Return a query that will select an approximate row count from the specified table.
     * See https://docs.microsoft.com/en-us/sql/relational-databases/system-dynamic-management-views/sys-dm-db-partition-stats-transact-sql?view=sql-server-ver15
     *
     * @param tableName
     * @param schemaName
     * @return
     */
    @Override
    List<String> approxCountQueryString(String tableName, String schemaName = null) {
        //use COUNT_BIG rather than COUNT
        String schemaIdentifier = schemaName ? "${escapeIdentifier(schemaName)}." : ""
        String fullTableName = "${schemaIdentifier}${escapeIdentifier(tableName)}"
        [
            """SELECT SUM(dm_db_partition_stats.row_count) AS approx_count
FROM sys.dm_db_partition_stats
WHERE object_id = OBJECT_ID('${fullTableName}')
AND (index_id = 0 OR index_id = 1)""".stripIndent(),
            "SELECT COUNT_BIG(*) AS approx_count FROM ${fullTableName} WITH (NOLOCK)".toString()
        ]
    }

    @Override
    String distinctColumnValuesQueryString(CalculationStrategy calculationStrategy, SamplingStrategy samplingStrategy, String columnName, String tableName,
                                           String schemaName = null, boolean allValues = false) {
        String schemaIdentifier = schemaName ? "${escapeIdentifier(schemaName)}." : ""
        String limitClause = allValues ? "" : "TOP (${calculationStrategy.maxEnumerations + 1})"
        """SELECT ${limitClause} ${escapeIdentifier(columnName)} AS distinct_value
FROM ${schemaIdentifier}${escapeIdentifier(tableName)} ${samplingStrategy.samplingClause(SamplingStrategy.Type.ENUMERATION_VALUES)}  WITH (NOLOCK)
WHERE ${escapeIdentifier(columnName)} <> ''
GROUP BY ${escapeIdentifier(columnName)}""".stripIndent()
    }

    @Override
    String columnRangeDistributionQueryString(SamplingStrategy samplingStrategy, DataType dataType,
                                              AbstractIntervalHelper intervalHelper,
                                              String columnName, String tableName, String schemaName) {
        List<String> selects = intervalHelper.intervals.collect {
            """SELECT
  '${it.key}' AS interval_label,
  ${formatDataType(dataType, it.value.aValue)} AS interval_start,
  ${formatDataType(dataType, it.value.bValue)} AS interval_end""".stripIndent()
        }

        rangeDistributionQueryString(samplingStrategy, selects, columnName, tableName, schemaName)
    }

    @Override
    String enumerationValueDistributionQueryString(SamplingStrategy samplingStrategy,
                                                   String columnName,
                                                   String tableName,
                                                   String schemaName) {

        """SELECT
  ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)} AS enumeration_value,
  ${samplingStrategy.scaleFactor()} * COUNT_BIG(*) AS enumeration_count
FROM ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)} ${samplingStrategy.samplingClause(SamplingStrategy.Type.SUMMARY_METADATA)} WITH (NOLOCK)
GROUP BY ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)}
ORDER BY ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)}""".stripIndent()
    }

    @Override
    String minMaxColumnValuesQueryString(SamplingStrategy samplingStrategy, String columnName, String tableName, String schemaName = null) {
        String schemaIdentifier = schemaName ? "${escapeIdentifier(schemaName)}." : ""
        """SELECT MIN(${escapeIdentifier(columnName)}) AS min_value,
MAX(${escapeIdentifier(columnName)}) AS max_value
FROM ${schemaIdentifier}${escapeIdentifier(tableName)} ${samplingStrategy.samplingClause(SamplingStrategy.Type.SUMMARY_METADATA)} WITH (NOLOCK)
WHERE ${escapeIdentifier(columnName)} IS NOT NULL""".stripIndent()
    }

    @Override
    String greaterThanOrEqualRowCountQueryString(String tableName, String schemaName) {
        """SELECT 'GTE' FROM ${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)} WITH (NOLOCK) HAVING COUNT(*) >= ?"""
    }

    /**
     * Returns a String that looks, for example, like this:
     * WITH #interval AS (
     *   SELECT '0 - 100' AS interval_label, 0 AS interval_start, 100 AS interval_end
     *   UNION
     *   SELECT '100 - 200' AS interval_label, 100 AS interval_start, 200 AS interval_end
     * )
     * SELECT interval_label, COUNT([my_column]) AS interval_count
     * FROM #interval
     * LEFT JOIN
     * [my_table] ON [my_table].[my_column] >= #interval.interval_start AND [my_table].[my_column] < #interval.interval_end
     * GROUP BY interval_label, interval_start
     * ORDER BY interval_start ASC;
     *
     * @param tableName
     * @param columnName
     * @param selects
     * @return
     */
    private String rangeDistributionQueryString(SamplingStrategy samplingStrategy, List<String> selects, String columnName,
                                                String tableName, String schemaName) {
        String intervals = selects.join("\nUNION\n")
        String column = "${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}.${escapeIdentifier(columnName)}"
        String table = "${escapeIdentifier(schemaName)}.${escapeIdentifier(tableName)}"
        "WITH intervals AS (\n${intervals}\n)" +
        """
SELECT
    interval_label,
    ${samplingStrategy.scaleFactor()} * COUNT_BIG(${escapeIdentifier(columnName)}) AS interval_count
FROM intervals WITH (NOLOCK)
LEFT JOIN ${table} ${samplingStrategy.samplingClause(SamplingStrategy.Type.SUMMARY_METADATA)}
    ON ${column} >= intervals.interval_start AND ${column} < intervals.interval_end
GROUP BY interval_label, interval_start
ORDER BY interval_start ASC;
        """.stripIndent()
    }

    /**
     * Return a string which uses the SQL Server CONVERT function for Dates, otherwise string formatting
     *
     * @param dataType
     * @param value
     * @return Date formatted as ISO8601 (see
     * https://docs.microsoft.com/en-us/sql/t-sql/functions/cast-and-convert-transact-sql?view=sql-server-ver15)
     * or a string
     */
    String formatDataType(DataType dataType, Object value) {
        if (isColumnForDateSummary(dataType)){
            "CONVERT(DATETIME, '${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value as TemporalAccessor)}', 126)"
        } else {
            "${value}"
        }
    }

    boolean isColumnForDateSummary(DataType dataType) {
        dataType.domainType == 'PrimitiveType' && ["date", "smalldatetime", "datetime", "datetime2"].contains(dataType.label)
    }

}
