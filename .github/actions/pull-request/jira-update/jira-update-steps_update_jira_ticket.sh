#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/scripts/jira.sh
}

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"
