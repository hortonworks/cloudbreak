#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/gradlew -Penv=jenkins :flow:test -PcomponentTest --no-daemon --quiet
  RESULT=$?
  if [[ $RESULT -eq 0 ]]; then
    echo "Completed successfully."
    else
    exit $RESULT
  fi
}

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"