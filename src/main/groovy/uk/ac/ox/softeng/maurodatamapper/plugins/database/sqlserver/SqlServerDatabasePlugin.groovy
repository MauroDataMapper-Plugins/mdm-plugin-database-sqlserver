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

import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table.SqlServerTableProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column.SqlServerColumnProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin

class SqlServerDatabasePlugin extends AbstractMauroDataMapperPlugin {

    @Override
    String getName() {
        'Plugin : Database - SQL Server'
    }

    @Override
    Closure doWithSpring() {
        {->
            sqlServerDatabaseDataModelImporterProviderService SqlServerDatabaseDataModelImporterProviderService
            sqlServerDataTypeProvider SqlServerDataTypeProvider
            sqlServerDatabaseProfileProviderService SqlServerDatabaseProfileProviderService
            sqlServerTableProfileProviderService SqlServerTableProfileProviderService
            sqlServerColumnProfileProviderService SqlServerColumnProfileProviderService
        }
    }
}
