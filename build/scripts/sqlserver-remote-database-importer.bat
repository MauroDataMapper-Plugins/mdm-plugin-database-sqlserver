@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  sqlserver-remote-database-importer startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and SQLSERVER_REMOTE_DATABASE_IMPORTER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-DapplicationVersion=1.12.1-SNAPSHOT" "-Dmc.logFileName=sqlserver-remote-database-importer" "-Dgrails.env=CUSTOM"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\mc-plugin-database-sqlserver-1.12.1-SNAPSHOT.jar;%APP_HOME%\lib\groovy-all-2.4.11.jar;%APP_HOME%\lib\mc-core-3.12.1-SNAPSHOT.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\groovy-2.4.11.jar;%APP_HOME%\lib\guava-23.0.jar;%APP_HOME%\lib\commons-lang3-3.6.jar;%APP_HOME%\lib\mc-plugin-database-1.12.1-SNAPSHOT.jar;%APP_HOME%\lib\mssql-jdbc-6.4.0.jre8.jar;%APP_HOME%\lib\jtds-1.3.1.jar;%APP_HOME%\lib\postgresql-42.1.4.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\spring-boot-starter-logging-1.5.7.RELEASE.jar;%APP_HOME%\lib\spring-boot-autoconfigure-1.5.7.RELEASE.jar;%APP_HOME%\lib\grails-core-3.3.1.jar;%APP_HOME%\lib\spring-boot-starter-actuator-1.5.7.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-1.5.7.RELEASE.jar;%APP_HOME%\lib\grails-plugin-url-mappings-3.3.1.jar;%APP_HOME%\lib\grails-plugin-rest-3.3.1.jar;%APP_HOME%\lib\grails-plugin-codecs-3.3.1.jar;%APP_HOME%\lib\grails-plugin-interceptors-3.3.1.jar;%APP_HOME%\lib\grails-plugin-services-3.3.1.jar;%APP_HOME%\lib\grails-plugin-datasource-3.3.1.jar;%APP_HOME%\lib\grails-plugin-databinding-3.3.1.jar;%APP_HOME%\lib\grails-web-boot-3.3.1.jar;%APP_HOME%\lib\grails-logging-3.3.1.jar;%APP_HOME%\lib\cache-4.0.0.jar;%APP_HOME%\lib\async-3.3.2.jar;%APP_HOME%\lib\grails-datastore-rest-client-6.1.9.OXBRC.jar;%APP_HOME%\lib\hibernate5-6.1.8.jar;%APP_HOME%\lib\hibernate-ehcache-5.2.10.Final.jar;%APP_HOME%\lib\hibernate-search-2.3.0.jar;%APP_HOME%\lib\grails-java8-1.2.2.jar;%APP_HOME%\lib\views-json-1.2.6.jar;%APP_HOME%\lib\views-json-templates-1.2.6.jar;%APP_HOME%\lib\views-markup-1.2.6.jar;%APP_HOME%\lib\utils-3.3.2.jar;%APP_HOME%\lib\flyway-core-4.2.0.jar;%APP_HOME%\lib\hibernate-search-orm-5.9.1.Final.jar;%APP_HOME%\lib\poi-4.1.1.jar;%APP_HOME%\lib\poi-ooxml-4.1.1.jar;%APP_HOME%\lib\poi-ooxml-schemas-4.1.1.jar;%APP_HOME%\lib\commons-rng-simple-1.0.jar;%APP_HOME%\lib\commons-text-1.1.jar;%APP_HOME%\lib\jcommander-1.72.jar;%APP_HOME%\lib\simple-java-mail-4.2.3.jar;%APP_HOME%\lib\commons-beanutils-1.9.3.jar;%APP_HOME%\lib\grails-plugin-domain-class-3.3.1.jar;%APP_HOME%\lib\el-impl-2.1.2-b03.jar;%APP_HOME%\lib\tomcat-jdbc-8.5.20.jar;%APP_HOME%\lib\h2-1.4.196.jar;%APP_HOME%\lib\error_prone_annotations-2.0.18.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.14.jar;%APP_HOME%\lib\commons-cli-1.3.1.jar;%APP_HOME%\lib\jackson-annotations-2.8.10.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.8.10.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.25.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.25.jar;%APP_HOME%\lib\log4j-over-slf4j-1.7.25.jar;%APP_HOME%\lib\spring-boot-1.5.7.RELEASE.jar;%APP_HOME%\lib\hibernate-jpa-2.1-api-1.0.0.Final.jar;%APP_HOME%\lib\concurrentlinkedhashmap-lru-1.4.2.jar;%APP_HOME%\lib\spring-core-4.3.11.RELEASE.jar;%APP_HOME%\lib\spring-tx-4.3.11.RELEASE.jar;%APP_HOME%\lib\spring-beans-4.3.11.RELEASE.jar;%APP_HOME%\lib\spring-context-4.3.11.RELEASE.jar;%APP_HOME%\lib\grails-bootstrap-3.3.1.jar;%APP_HOME%\lib\grails-spring-3.3.1.jar;%APP_HOME%\lib\grails-datastore-core-6.1.9.OXBRC.jar;%APP_HOME%\lib\serializer-2.7.2.jar;%APP_HOME%\lib\spring-boot-starter-1.5.7.RELEASE.jar;%APP_HOME%\lib\spring-boot-actuator-1.5.7.RELEASE.jar;%APP_HOME%\lib\tomcat-embed-core-8.5.20.jar;%APP_HOME%\lib\tomcat-embed-el-8.5.20.jar;%APP_HOME%\lib\tomcat-embed-websocket-8.5.20.jar;%APP_HOME%\lib\grails-web-3.3.1.jar;%APP_HOME%\lib\grails-plugin-controllers-3.3.1.jar;%APP_HOME%\lib\grails-validation-3.3.1.jar;%APP_HOME%\lib\converters-3.3.1.jar;%APP_HOME%\lib\grails-encoder-3.3.1.jar;%APP_HOME%\lib\grails-codecs-3.3.1.jar;%APP_HOME%\lib\spring-jdbc-4.3.11.RELEASE.jar;%APP_HOME%\lib\groovy-sql-2.4.11.jar;%APP_HOME%\lib\grails-datastore-gorm-6.1.9.OXBRC.jar;%APP_HOME%\lib\tomcat-embed-logging-log4j-8.5.2.jar;%APP_HOME%\lib\grails-web-common-3.3.1.jar;%APP_HOME%\lib\events-3.3.2.jar;%APP_HOME%\lib\grails-async-3.3.2.jar;%APP_HOME%\lib\grails-plugin-converters-3.2.11.jar;%APP_HOME%\lib\grails-datastore-web-6.1.9.OXBRC.jar;%APP_HOME%\lib\grails-datastore-gorm-support-6.1.9.OXBRC.jar;%APP_HOME%\lib\grails-datastore-gorm-hibernate5-6.1.8.RELEASE.jar;%APP_HOME%\lib\spring-orm-4.3.11.RELEASE.jar;%APP_HOME%\lib\ehcache-2.10.3.jar;%APP_HOME%\lib\views-core-1.2.6.jar;%APP_HOME%\lib\gson-2.8.5.jar;%APP_HOME%\lib\javax.servlet-api-3.1.0.jar;%APP_HOME%\lib\org.eclipse.persistence.moxy-2.7.0.jar;%APP_HOME%\lib\hibernate-search-engine-5.9.1.Final.jar;%APP_HOME%\lib\commons-collections4-4.4.jar;%APP_HOME%\lib\commons-math3-3.6.1.jar;%APP_HOME%\lib\commons-compress-1.19.jar;%APP_HOME%\lib\curvesapi-1.06.jar;%APP_HOME%\lib\xmlbeans-3.1.0.jar;%APP_HOME%\lib\commons-rng-core-1.0.jar;%APP_HOME%\lib\javax.mail-1.5.5.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-collections-3.2.2.jar;%APP_HOME%\lib\grails-datastore-gorm-validation-6.1.9.OXBRC.jar;%APP_HOME%\lib\el-api-2.1.2-b03.jar;%APP_HOME%\lib\tomcat-juli-8.5.20.jar;%APP_HOME%\lib\jackson-core-2.8.10.jar;%APP_HOME%\lib\jackson-databind-2.8.10.jar;%APP_HOME%\lib\spring-aop-4.3.11.RELEASE.jar;%APP_HOME%\lib\spring-expression-4.3.11.RELEASE.jar;%APP_HOME%\lib\groovy-xml-2.4.11.jar;%APP_HOME%\lib\groovy-templates-2.4.11.jar;%APP_HOME%\lib\spring-web-4.3.11.RELEASE.jar;%APP_HOME%\lib\jta-1.1.jar;%APP_HOME%\lib\grails-web-databinding-3.3.1.jar;%APP_HOME%\lib\grails-web-fileupload-3.3.1.jar;%APP_HOME%\lib\grails-web-url-mappings-3.3.1.jar;%APP_HOME%\lib\grails-web-mvc-3.3.1.jar;%APP_HOME%\lib\grails-web-gsp-3.3.0.jar;%APP_HOME%\lib\grails-web-sitemesh-3.3.0.jar;%APP_HOME%\lib\grails-plugin-mimetypes-3.3.1.jar;%APP_HOME%\lib\grails-plugin-validation-3.3.1.jar;%APP_HOME%\lib\grails-plugin-i18n-3.3.1.jar;%APP_HOME%\lib\commons-lang-2.6.jar;%APP_HOME%\lib\groovy-json-2.4.11.jar;%APP_HOME%\lib\grails-databinding-3.3.1.jar;%APP_HOME%\lib\grails-gsp-3.3.0.jar;%APP_HOME%\lib\spring-webmvc-4.3.11.RELEASE.jar;%APP_HOME%\lib\spring-context-support-4.3.11.RELEASE.jar;%APP_HOME%\lib\grails-events-3.3.2.jar;%APP_HOME%\lib\grails-events-transform-3.3.2.jar;%APP_HOME%\lib\grails-events-compat-3.3.2.jar;%APP_HOME%\lib\grails-datastore-gorm-hibernate-core-6.1.8.RELEASE.jar;%APP_HOME%\lib\hibernate-validator-5.2.5.Final.jar;%APP_HOME%\lib\javax.el-api-2.2.4.jar;%APP_HOME%\lib\org.eclipse.persistence.core-2.7.0.jar;%APP_HOME%\lib\validation-api-1.1.0.Final.jar;%APP_HOME%\lib\javax.json-1.0.4.jar;%APP_HOME%\lib\hibernate-commons-annotations-5.0.1.Final.jar;%APP_HOME%\lib\lucene-core-5.5.5.jar;%APP_HOME%\lib\lucene-misc-5.5.5.jar;%APP_HOME%\lib\lucene-analyzers-common-5.5.5.jar;%APP_HOME%\lib\lucene-facet-5.5.5.jar;%APP_HOME%\lib\lucene-queryparser-5.5.5.jar;%APP_HOME%\lib\commons-rng-client-api-1.0.jar;%APP_HOME%\lib\activation-1.1.jar;%APP_HOME%\lib\commons-validator-1.5.1.jar;%APP_HOME%\lib\commons-fileupload-1.3.2.jar;%APP_HOME%\lib\grails-web-taglib-3.3.0.jar;%APP_HOME%\lib\sitemesh-2.4.jar;%APP_HOME%\lib\grails-taglib-3.3.0.jar;%APP_HOME%\lib\org.eclipse.persistence.asm-2.7.0.jar;%APP_HOME%\lib\lucene-queries-5.5.5.jar;%APP_HOME%\lib\commons-io-2.2.jar;%APP_HOME%\lib\hibernate-core-5.2.12.Final.jar;%APP_HOME%\lib\antlr-2.7.7.jar;%APP_HOME%\lib\jboss-transaction-api_1.2_spec-1.0.1.Final.jar;%APP_HOME%\lib\jandex-2.0.3.Final.jar;%APP_HOME%\lib\dom4j-1.6.1.jar;%APP_HOME%\lib\commons-codec-1.13.jar;%APP_HOME%\lib\asset-pipeline-grails-2.14.8.jar;%APP_HOME%\lib\jsr305-3.0.1.jar;%APP_HOME%\lib\javassist-3.21.0-GA.jar;%APP_HOME%\lib\snakeyaml-1.17.jar;%APP_HOME%\lib\jboss-logging-3.3.1.Final.jar;%APP_HOME%\lib\classmate-1.3.0.jar;%APP_HOME%\lib\asset-pipeline-core-2.14.8.jar

@rem Execute sqlserver-remote-database-importer
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SQLSERVER_REMOTE_DATABASE_IMPORTER_OPTS%  -classpath "%CLASSPATH%" ox.softeng.metadatacatalogue.plugins.database.sqlserver.SqlServerDatabaseImporterService %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SQLSERVER_REMOTE_DATABASE_IMPORTER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SQLSERVER_REMOTE_DATABASE_IMPORTER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
