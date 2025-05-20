#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/gradlew -Penv=jenkins -b build.gradle --quiet \
    check \
    -x spotbugsTest \
    -x checkstyleTest \
    -x checkstyleMain \
    -x test \
    --no-daemon --quiet --parallel -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
}

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"