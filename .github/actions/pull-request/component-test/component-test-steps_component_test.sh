#!/bin/bash -ex

main() {
  set -ex -o pipefail

  set +e
  $(pwd)/gradlew -Penv=jenkins :flow:test :core:test -PcomponentTest --continue --no-daemon
  RESULT=$?
  set -e
  CHECK_RESULT=0
  (cd flow && ./scripts/check-results.sh) || CHECK_RESULT=1
  (cd core && ../flow/scripts/check-results.sh) || CHECK_RESULT=1
  if [[ $RESULT -eq 0 && $CHECK_RESULT -eq 0 ]]; then
    echo "Completed successfully."
  elif [[ $RESULT -ne 0 ]]; then
    exit $RESULT
  else
    exit $CHECK_RESULT
  fi
}

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"
