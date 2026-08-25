#!/bin/bash -ex

main() {
  set -ex -o pipefail

  ./gradlew -Penv=jenkins -b build.gradle build \
    -x test \
    -x checkstyleMain \
    -x checkstyleTest \
    -x spotbugsMain \
    -x spotbugsTest \
    -x spotlessApply \
    -x spotlessCheck --no-daemon -PintegrationTest >> build.log

  rm -rf integration-test/integcb/.deps
  rm -rf integration-test/integcb/.schema

  cd integration-test
  docker rm -f $(docker ps -aq) || true

  export PRIMARYKEY_CHECK=true
  # Container resource ceilings for the mock IT ONLY. This job runs on cb-ubuntu22-8xlarge
  # (32 vCPU / 128 GiB, RELENG-36083) with the whole node effectively to itself (dind is
  # uncapped at the K8s level), so lift the shared Profile_template baselines here -- NOT in the
  # template, which is also used by jobs on the smaller cb-ubuntu22-large runners. cloudbreak core
  # is the hot path under 24 parallel test methods, so it gets the largest share; the higher
  # service ceiling lets environment/datalake/freeipa burst. mock-infrastructure is the cloud backend
  # every test funnels through -- at the deployer default (1 CPU / 768M) it pinned flat at ~1 core and
  # serialised the whole suite, so it gets a lifted cap too. These appends land after the template's
  # own exports, so they win.
  echo "export CPUS_FOR_CLOUDBREAK=12.0" >> integcb/Profile_template
  echo "export CPUS_FOR_SERVICES=8.0" >> integcb/Profile_template
  echo "export MEMORY_FOR_OTHER_SERVICES=4096M" >> integcb/Profile_template
  echo "export CPUS_FOR_MOCK_INFRASTRUCTURE=8.0" >> integcb/Profile_template
  echo "export MEMORY_FOR_MOCK_INFRASTRUCTURE=2048M" >> integcb/Profile_template
  echo "export COMMON_DB_VOL=${GITHUB_RUN_ATTEMPT}-sm" >> integcb/Profile_template
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

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"
