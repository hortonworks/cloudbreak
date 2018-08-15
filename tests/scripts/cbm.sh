#!/usr/bin/env bash

: ${GIT_VERSION:=latest}
: ${GIT_TAG:=latest}
: ${BASE_URL:=https://127.0.0.1}

mock-image-tag() {
    declare desc="Set Cloudbreak Mock Docker image tag"

    echo "GitHub First Parent Tag is: ${GIT_VERSION}"
    echo "GitHub Tag is: ${GIT_TAG}"
    echo "CircleCI Branch is: ${CIRCLE_BRANCH}"
    echo "CircleCI Tag is: ${CIRCLE_TAG}"

    if [[ "${GIT_VERSION}" != *"-rc."* ]]; then
        export GIT_VERSION=latest
    fi
}

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

    docker-compose -f docker-compose.yml -p cbreak up -d
    sleep 30s
}

main() {
    mock-image-tag
    mock-start
    mock-start-logs
}

main "$@"
