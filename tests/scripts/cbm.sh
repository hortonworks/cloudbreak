#!/usr/bin/env bash

: ${GIT_VERSION:=latest}
: ${GIT_TAG:=latest}

set-mock-image-tag() {
    echo "GitHub First Parent Tag is: ${GIT_VERSION}"
    echo "GitHub Tag is: ${GIT_TAG}"
    echo "CircleCI Branch is: ${CIRCLE_BRANCH}"
    echo "CircleCI Tag is: ${CIRCLE_TAG}"

    if [[ "${GIT_VERSION}" != *"-rc."* ]]; then
        export GIT_VERSION=latest
    fi
}

set-mock-image-tag
docker-compose up -d

sleep 30s
