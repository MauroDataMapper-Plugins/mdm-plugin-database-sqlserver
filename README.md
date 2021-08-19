# mdm-plugin-database-sqlserver

SQL Server Plugin for the Mauro Data Mapper

| Branch | Build Status |
| ------ | ------------ |
| main | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-database-sqlserver%2Fmain)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-database-sqlserver/branches) |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-database-sqlserver%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-database-sqlserver/branches) |

## Requirements

* Java 12 (AdoptOpenJDK)
* Grails 4.0.3+
* Gradle 6.5+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).

## Applying the Plugin

The preferred way of running Mauro Data Mapper is using the [mdm-docker](https://github.com/MauroDataMapper/mdm-docker) deployment. However you can
also run the backend on its own from [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build).

### mdm-docker

In the `docker-compose.yml` file add:

```yml
mauro-data-mapper:
    build:
        args:
            ADDITIONAL_PLUGINS: "uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-database-sqlserver:5.0.0"
```

Please note, if adding more than one plugin, this is a semicolon-separated list

### mdm-application-build

In the `dependencies.gradle` file add:

```groovy
dependencies {
    runtimeOnly 'uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-database-sqlserver:5.0.0'
}
```

## Development

When making changes to this plugin you may also need to make changes to ```mdm-plugin-database```. To work locally on both projects
you can add e.g. the following as the first entry of settings.gradle in this project:

```
pluginManagement {
    includeBuild '../mdm-plugin-database'
}

```
(The above assumes that both this project and ```mdm-plugin-database``` are located in the same directory).

If using Intellij, go to Preferences > Build Tools and choose to reload the project after Any changes.

To run tests:
```./gradlew --build-cache integrationTest```

With debug:
```./gradlew --build-cache integrationTest --debug-jvm```  