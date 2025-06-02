#!/bin/bash -ex

main() {
  set -ex -o pipefail

  ./gradlew -Penv=jenkins -b build.gradle build \
    -x test \
    -x checkstyleMain \
    -x checkstyleTest \
    -x spotbugsMain \
    -x spotbugsTest --no-daemon -PintegrationTest

  rm -rf integration-test/integcb/.deps
  rm -rf integration-test/integcb/.schema
  cd integration-test

  export PRIMARYKEY_CHECK=true
  echo "export CPUS_FOR_CLOUDBREAK=4.0" >> integcb/Profile_template
  echo "export COMMON_DB_VOL=${GITHUB_RUN_ATTEMPT}-fed" >> integcb/Profile_template
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

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"