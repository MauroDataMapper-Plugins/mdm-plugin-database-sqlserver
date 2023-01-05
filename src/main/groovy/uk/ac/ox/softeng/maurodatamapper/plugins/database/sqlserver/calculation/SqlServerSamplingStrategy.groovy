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
package uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.calculation

import uk.ac.ox.softeng.maurodatamapper.plugins.database.DatabaseDataModelWithSamplingImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.database.calculation.SamplingStrategy

import org.apache.commons.lang3.RandomUtils

class SqlServerSamplingStrategy extends SamplingStrategy {

    // Allows the same data to be returned each time the same query is called on the table.
    // This will allow consistency when asking multiple questions of the same dataset
    private int repeatSeed

    SqlServerSamplingStrategy(String schema, String table, DatabaseDataModelWithSamplingImporterProviderServiceParameters samplingImporterProviderServiceParameters) {
        super(schema, table, samplingImporterProviderServiceParameters)
        repeatSeed = RandomUtils.nextInt(0, Integer.MAX_VALUE)
    }

    /**
     * SQL Server can only use TABLESAMPLE on tables
     * @return
     */
    boolean canSampleTableType() {
        this.tableType == 'BASE TABLE'
    }

    /**
     * SQL Server specific TABLESAMPLE clause
     * @return
     */
    @Override
    String samplingClause(Type type) {
        BigDecimal percentage
        switch (type) {
            case Type.SUMMARY_METADATA:
                percentage = getSummaryMetadataSamplePercentage()
                break
            case Type.ENUMERATION_VALUES:
                percentage = getEnumerationValueSamplePercentage()
                break
        }
        this.useSamplingFor(type) ? " TABLESAMPLE (${percentage} PERCENT) REPEATABLE (${this.repeatSeed}) " : ''
    }
}
