#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/gradlew -Penv=jenkins -b build.gradle --quiet \
    check \
    -x spotbugsMain \
    -x spotbugsTest \
    -x checkstyleTest \
    -x test \
    --no-daemon --parallel -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
}

source $(pwd)/.github/steps/prerequisites.sh
main "$@"