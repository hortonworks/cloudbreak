#!/usr/bin/env bash

: ${GIT_VERSION:=latest}
: ${BASE_URL:=https://127.0.0.1}

mock-start-logs() {
    declare desc="Gather Cloudbreak Mock start logs"

    mkdir -pv test_log

    if [[ $(id -u jenkins 2>/dev/null || echo $?) -gt 1 ]]; then
        sudo chown -R jenkins .
        sudo docker logs cbreak_cloudbreak_1 > test_log/cloudbreak_start.log
    else
        docker logs cbreak_cloudbreak_1 > test_log/cloudbreak_start.log
    fi
}

mock-start() {
    declare desc="Start Cloudbreak Mock"

    echo "hortonworks/cloudbreak-mock-caas image version is: ${GIT_VERSION}"

    docker-compose -f tmp/docker-compose.yml -p cbreak up -d
    sleep 30s
}

main() {
    mock-start
    mock-start-logs
}

main "$@"