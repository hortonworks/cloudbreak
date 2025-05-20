#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/scripts/jira.sh
}

source $(pwd)/.github/workflows/pull-request/steps/prerequisites.sh
main "$@"