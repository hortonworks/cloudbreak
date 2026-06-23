#!/bin/bash -ex

main() {
  set -ex -o pipefail
  source $(pwd)/.github/actions/pull-request/changed-modules.sh
  TASKS=$(changed_module_tasks checkstyleMain)
  if [ -z "$TASKS" ]; then
    echo "No changed Gradle modules; skipping checkstyleMain."
    exit 0
  fi
  echo "Running checkstyleMain for:$TASKS"
  $(pwd)/gradlew -Penv=jenkins -b build.gradle --quiet \
    $TASKS \
    --no-daemon --quiet --parallel -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
}

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"
