#!/bin/bash -ex

main() {
  cd $(pwd)/integration-test
  VERSION=$(get_latest_version) TARGET_BRANCH=$BRANCH make swagger-check
}

source $(pwd)/.github/steps/prerequisites.sh
main "$@"