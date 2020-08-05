#!/bin/bash

docker run --rm -d \
-e 'ACCEPT_EULA=Y' \
-e 'SA_PASSWORD=yourStrong(!)Password' \
-v /data/docker_volumes/sqlserver:/var/opt/mssql \
-p 1433:1433 \
--name sqlserver2017 \
microsoft/mssql-server-linux:2017-latest