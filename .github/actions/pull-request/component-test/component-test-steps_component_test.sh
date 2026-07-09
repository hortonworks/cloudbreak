#!/bin/bash -ex

main() {
  set -ex -o pipefail

  set +e
  $(pwd)/gradlew -Penv=jenkins :flow:test -PcomponentTest --no-daemon
  RESULT=$?
  set -e
  cd flow
  ./scripts/check-results.sh
  if [[ $RESULT -eq 0 ]]; then
    echo "Completed successfully."
  else
    exit $RESULT
  fi
}

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"
