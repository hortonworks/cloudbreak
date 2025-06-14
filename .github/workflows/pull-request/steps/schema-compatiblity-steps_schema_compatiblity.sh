#!/bin/bash -ex

main() {
  set -ex -o pipefail

  PRODUCTION_VERSION=$(curl "https://console.us-west-1.cdp.cloudera.com/cloud/cb/info" |jq  -r '.app.version')
  echo "The schema_compatibility_version is:::"
  echo $PRODUCTION_VERSION

  x=0
  for c in `curl -sH "Accept: application/json" https://github.infra.cloudera.com/api/v3/repos/cloudbreak/cloudbreak/pulls/$PULL_REQUEST_NUMBER/commits | jq -r '.[].sha'`
  do
    echo $(git show --name-only $c)
    echo $(git show --name-only $c|grep -Fc ".sql")
    x=$(( $x + $(git show --name-only $c | awk '/\.sql/ {count++} END {print count+0}') ));
  done

  echo "Number of sql string found in commits: $x"
  if [ $x -ne 0 ]; then
    echo "build cb"
    $(pwd)/gradlew -Penv=jenkins -b build.gradle build --quiet --no-daemon --parallel --warning-mode none \
      -x test \
      -x checkstyleMain \
      -x checkstyleTest \
      -x spotbugsMain \
      -x spotbugsTest >> build.log

    rm -rf integration-test/integcb/.deps
    rm -rf integration-test/integcb/.schema

    cd integration-test
    echo "Bring up schema with target $BRANCH "
    VERSION=$(get_latest_version) TARGET_BRANCH=$BRANCH make bring-up-schema
    cd ..

    CB_VERSION=$(echo $PRODUCTION_VERSION)
    echo "Version to checkout: '$CB_VERSION'"

    export PRIMARYKEY_CHECK=false && \
    git checkout -f $CB_VERSION && \
    $(pwd)/gradlew -Penv=jenkins -b build.gradle build --quiet --no-daemon --parallel --warning-mode none \
      -x test \
      -x checkstyleMain \
      -x checkstyleTest \
      -x spotbugsMain \
      -x spotbugsTest >> build.log && \
    cd integration-test/ && \
    docker rm -f $(docker ps -aq) || true

    echo "export CPUS_FOR_CLOUDBREAK=4.0" >> integcb/Profile_template
    export TARGET_BRANCH=CB-$(echo $PRODUCTION_VERSION | sed -E "s/-b[0-9]+//g")
    make without-build && make stop-containers
  else
  	echo "No SQL found in the PR. Interrupting test"
  fi
}

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"