#!/bin/bash -ex

main() {
set -ex -o pipefail

  export INTEGRATIONTEST_AUTHDISTRIBUTOR_HOST=thunderhead-mock
  export INTEGRATIONTEST_THREADCOUNT=4
  export RESTARTABLE_SERVICES="cloudbreak"

  gh_label=$(curl -H "Accept: application/json" https://github.infra.cloudera.com/api/v3/repos/cloudbreak/cloudbreak/pulls/$PULL_REQUEST_NUMBER |jq '.labels[]?|select(.name=="aws_e2e_test")|.name')
  if [[ -z "$gh_label" ]]
  then
    echo "no need for build"
  else
    echo "Move private key to the workspace for SSH tests"
    mkdir -p integration-test

    echo $SEQ_PRIV_FILE >> seq-master.pem
    cp -Rv seq-master.pem integration-test/
    SEQ_PRIV_FILE_PATH=$(find . -type f -iname "seq-master.pem")
    chmod 600 $SEQ_PRIV_FILE_PATH
    
    export SEQ_PRIV_FILE_NAME=$(basename "$SEQ_PRIV_FILE_PATH")
    export INTEGRATIONTEST_DEFAULTPRIVATEKEYFILE="/it/$SEQ_PRIV_FILE_NAME"
    export INTEGRATIONTEST_SSHPUBLICKEY="$(cat $SEQ_PUB_FILE)"
    
    echo "will trigger aws build"
    
    export INTEGRATIONTEST_SUITE_FILES=file:/it/src/main/resources/testsuites/e2e/aws-pr-tests.yaml
    full_suite_label=$(curl -H "Accept: application/json" https://github.infra.cloudera.com/api/v3/repos/cloudbreak/cloudbreak/pulls/$PULL_REQUEST_NUMBER |jq '.labels[]?|select(.name=="full_e2e_suite")|.name')
    if [[ -z "$full_suite_label" ]]
    then
      echo "no overriding label was provided, using configured suite"
    else
      export INTEGRATIONTEST_SUITE_FILES=file:/it/src/main/resources/testsuites/e2e/aws-distrox-tests.yaml
    fi
  
    echo "Run this test file: $INTEGRATIONTEST_SUITE_FILES"
  
    ./gradlew -Penv=jenkins -b build.gradle build \
      -x test \
      -x checkstyleMain \
      -x checkstyleTest \
      -x spotbugsMain \
      -x spotbugsTest --no-daemon

    cd integration-test

    export PRIMARYKEY_CHECK=true
    echo "export CPUS_FOR_CLOUDBREAK=4.0" >> integcb/Profile_template
    VERSION=$(get_latest_version) TARGET_BRANCH=$BRANCH make without-build
    RESULT=$?
    if [[ $(sudo find integration-test/dumps -name "*.hprof" | tail -1) ]]; then
      sudo cp -v $(sudo find integration-test/dumps -name "*.hprof" | tail -1) .
      sudo chown -R $(whoami) integration-test/dumps/*.hprof
    fi
    if [[ $RESULT -eq 0 ]]; then
      make stop-containers
    else
      exit $RESULT
    fi
  fi
}

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"
