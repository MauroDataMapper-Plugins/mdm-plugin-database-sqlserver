#!/bin/bash

docker run --rm -d \
-e 'ACCEPT_EULA=Y' \
-e 'SA_PASSWORD=yourStrong(!)Password' \
-p 1433:1433 \
--name sqlserver2019 \
mcr.microsoft.com/mssql/server:2019-latest