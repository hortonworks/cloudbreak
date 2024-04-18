#!/bin/bash -ex

main() {
  set -ex -o pipefail
  $(pwd)/scripts/jira.sh
}

source $(pwd)/.github/steps/prerequisites.sh
main "$@"