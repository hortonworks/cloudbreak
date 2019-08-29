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

new_version() {
  git checkout $VERSION
  debug "building docker image for version: $VERSION"

  # Build docker and push to hortonworks repo
  docker build -t ${DOCKER_IMAGE}:${VERSION} --build-arg=REPO_URL=${NEXUS_URL} --build-arg=VERSION=${VERSION} .
  docker push ${DOCKER_IMAGE}:${VERSION}
  docker rmi ${DOCKER_IMAGE}:${VERSION}
}

main() {
  : ${VERSION:?"required!"}
  : ${DOCKER_IMAGE:?"required!"}
  : ${DOCKERHUB_USERNAME:?"required!"}
  : ${DOCKERHUB_PASSWORD:?"required!"}
  : ${DEBUG:=1}
  : ${NEXUS_URL:?"required!"}

  new_version "$@"
}

[[ "$0" ==  "$BASH_SOURCE" ]] && main "$@"