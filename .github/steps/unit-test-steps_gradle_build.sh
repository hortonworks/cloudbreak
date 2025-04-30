#!/bin/bash -ex

main() {
  $(pwd)/gradlew -q javaToolchains

  $(pwd)/gradlew -Penv=jenkins -b build.gradle \
    test \
    jacocoTestReport \
    -x checkstyleMain \
    -x checkstyleTest \
    -x spotbugsMain \
    -x spotbugsTest \
    --no-daemon --quiet --parallel -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=256m -XX:+HeapDumpOnOutOfMemoryError"
}

source $(pwd)/.github/steps/prerequisites.sh
main "$@"