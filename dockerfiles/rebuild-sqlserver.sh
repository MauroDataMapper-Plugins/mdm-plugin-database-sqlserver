#!/bin/bash

# This script is designed to get around SQL Servers asinine license setup on its database
# As we're using docker and we don't actually care about the data in the database we can scrap and rebuild the volume
# This can take a while and should only be done when the warning about password change appears

# Stop any running containers
echo 'Stopping running container'
docker stop sqlserver2017

# Remove the old database
echo 'Removing the old database files'
sudo rm -rf /data/docker_volumes/sqlserver/*

# Start oracle container
echo 'Starting sqlserver2017'
/usr/local/bin/start-sqlserver

# Follow the logs until the db is built
echo 'Building new sqlserver database'
echo '>> Press ctrl+c when the database is built to continue this script'
echo '>> When the database is built please run /usr/local/bin/install-testdb-sqlserver'
echo ''
docker logs -f sqlserver2017
