#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.sensorDataService.SensorDataService.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}

# check mandatory variables
[[ -z "${MYSQL_USER}" ]] && \
    echo "Mandatory variable MYSQL_USER is not set. Add -e MYSQL_USER=mysqlUser to your arguments." && exit 1
[[ -z "${MYSQL_PASSWORD}" ]] && \
    echo "Mandatory variable MYSQL_PASSWORD is not set. Add -e MYSQL_PASSWORD=mysqlPassword to your arguments." && exit 1
[[ -z "${MYSQL_HOST}" ]] && \
    echo "Mandatory variable MYSQL_HOST is not set. Add -e MYSQL_HOST=mysqlHost to your arguments." && exit 1
[[ -z "${MYSQL_PORT}" ]] && \
    echo "Mandatory variable MYSQL_PORT is not set. Add -e MYSQL_PORT=mysqlPort to your arguments." && exit 1
[[ -z "${MYSQL_DATABASE}" ]] && \
    echo "Mandatory variable MYSQL_DATABASE is not set. Add -e MYSQL_DATABASE=mysqlDatabase to your arguments." && exit 1


# configure service properties
function set_in_service_config {
    sed -i "s#${1}[[:blank:]]*=.*#${1}=${2}#g" ${SERVICE_PROPERTY_FILE}
}

set_in_service_config mysqlUser ${MYSQL_USER}
set_in_service_config mysqlPassword ${MYSQL_PASSWORD}
set_in_service_config mysqlHost ${MYSQL_HOST}
set_in_service_config mysqlPort ${MYSQL_PORT}
set_in_service_config mysqlDatabase ${MYSQL_DATABASE}


# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* i5.las2peer.tools.L2pNodeLauncher -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
  exec ${LAUNCH_COMMAND} uploadStartupDirectory startService\("'""${SERVICE}""'"\) startWebConnector
else
  exec ${LAUNCH_COMMAND} ${@}
fi
