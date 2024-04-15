#!/bin/bash

set -eo pipefail
if [[ "$TRACE" ]]; then
    : ${START_TIME:=$(date +%s)}
    export START_TIME
    export PS4='+ [TRACE $BASH_SOURCE:$LINENO][ellapsed: $(( $(date +%s) -  $START_TIME ))] '
    set -x
fi

debug() {
  [[ "$DEBUG" ]] && echo "-----> $*" 1>&2 || :
}

scan() {
  debug "scanning started with version: $VERSION"

  docker run \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v "${PWD}"/aquasec:/tmp \
    ${AQUASEC_DOCKER_IMAGE} \
    scan -H ${AQUASEC_URL} \
    -U ${AQUASEC_USERNAME} -P ${AQUASEC_PASSWORD} \
    --htmlfile /tmp/result.html \
    --local ${DOCKER_IMAGE}:${VERSION}

  debug "scanning finished: $VERSION"
}

main() {
  : ${VERSION:?"required!"}
  : ${DOCKER_IMAGE:?"required!"}
  : ${AQUASEC_DOCKER_IMAGE:?"required!"}
  : ${AQUASEC_URL:?"required!"}
  : ${AQUASEC_USERNAME:?"required!"}
  : ${AQUASEC_PASSWORD:?"required!"}
  : ${DEBUG:=1}

  scan "$@"
}

[[ "$0" ==  "$BASH_SOURCE" ]] && main "$@"