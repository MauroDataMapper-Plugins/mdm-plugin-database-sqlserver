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


import grails.plugins.Plugin

class MdmPluginDatabaseSqlserverGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = '5.1.7 > *'
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
    ]

    def title = "SQL Server Database Plugin"
    // Headline display name of the plugin
    def author = "Oliver Freeman"
    def authorEmail = "oliver.freeman@bdi.ox.ac.uk"
    def description = '''\
Imports SQL Server databases into MauroDataMapper
'''

    // URL to the plugin's documentation
    def documentation = ""

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [name: "Oxford University BRC Informatics", url: "www.ox.ac.uk"]

    // Any additional developers beyond the author specified above.
    def developers = [[name: 'Oliver Freeman', email: 'oliver.freeman@bdi.ox.ac.uk'],
        [name: 'James Welch', email: 'james.welch@bdi.ox.ac.uk'],
                      [name: 'Aaron Forshaw', email: 'aaron.forshaw@swiftxf.co.uk']]

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "YouTrack", url: "https://maurodatamapper.myjetbrains.com"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/mauroDataMapper-plugins/mdm-plugin-database-sqlserver"]

    def dependsOn = [
        mdmCore    : '5.0.0 > *',
    ]

    Closure doWithSpring() {
        {->
        }
    }

    void doWithDynamicMethods() {
    }

    void doWithApplicationContext() {
    }

    void onChange(Map<String, Object> event) {
    }

    void onConfigChange(Map<String, Object> event) {
    }

    void onShutdown(Map<String, Object> event) {
    }
}
