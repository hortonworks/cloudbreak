#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/gradlew -Penv=jenkins -b build.gradle build --quiet --warning-mode none \
    -x test \
    -x checkstyleMain \
    -x checkstyleTest \
    -x spotbugsMain \
    -x spotbugsTest \
    --no-daemon --quiet --parallel -PintegrationTest >> build.log
  cd $(pwd)/integration-test

  export PRIMARYKEY_CHECK=true
  VERSION=$(get_latest_version) TARGET_BRANCH=$BRANCH make without-build
  RESULT=$?
  if [[ $(sudo find integration-test/dumps -name "*.hprof" | tail -1) ]]; then
  sudo cp -v $(sudo find integration-test/dumps -name "*.hprof" | tail -1) .
  sudo chown -R $(whoami) integration-test/dumps/*.hprof
  fi
  if [[ $RESULT -eq 0 ]]; then
  make revert-db
  make stop-containers
  else
  exit $RESULT
  fi
}

source $(pwd)/.github/steps/prerequisites.sh
main "$@"