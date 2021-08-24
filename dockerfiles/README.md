# Setting up SQL Server (MSSQL) Test Environment

## Start the SqlServer Docker Instance

The following command will start up a default MSSQL server instance.

```bash
docker run --rm -d \
 -e 'ACCEPT_EULA=Y' \
 -e 'SA_PASSWORD=yourStrong(!)Password' 
 -p 1433:1433 
 --name sqlserver2019
 mcr.microsoft.com/mssql/server:2019-latest
```

Note that the above image does not run on Apple silicon (M1). Use azure-sql-edge instead:
```bash
docker run --rm -d -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=yourStrong(!)Password' -p 1433:1433 --name sqlserver2019 mcr.microsoft.com/azure-sql-edge:latest
 ```

The available tags can be found [here](https://hub.docker.com/_/microsoft-mssql-server) if you want a different version.

## Install SqlCmd 

Full Instructions can be found here

* [Unix](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-ver15#macos)
* [Windows](https://docs.microsoft.com/en-us/sql/tools/sqlcmd-utility?view=sql-server-ver15)

### Install SqlCmd on Ubuntu

Instructions [here](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-ver15#ubuntu)

```bash
$ curl https://packages.microsoft.com/keys/microsoft.asc | sudo apt-key add -
$ curl https://packages.microsoft.com/config/ubuntu/16.04/prod.list | sudo tee /etc/apt/sources.list.d/msprod.list
$ sudo apt-get update
$ sudo apt-get install mssql-tools unixodbc-dev
$ echo 'export PATH="$PATH:/opt/mssql-tools/bin"' >> ~/.bash_profile
$ source ~/.bashrc
```

### Install SqlCmd on Apple Mac OS X

Instructions [here](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-ver15#macos)

```bash
$ brew tap microsoft/mssql-release https://github.com/Microsoft/homebrew-mssql-release
$ brew update
$ HOMEBREW_NO_ENV_FILTERING=1 ACCEPT_EULA=y brew install mssql-tools
```

## To install the simple database in an MSSQL server.

```bash
$ sqlcmd -U sa -P 'yourStrong(!)Password' -i fixtures/create_metadata_simple.sql
```

## Jenkins Testing Environment

On jenkins machine there are the following scripts in `/usr/local/bin`

* install-testdb-sqlserver
* rebuild-sqlserver.sh
* start-sqlserver.sh