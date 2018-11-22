#!/usr/bin/env bash
source format-and-mount-common.sh

# INPUT
#   nothing
#
# OUTPUT
#   SEMAPHORE file
#   STDOUT: nothing


main() {
    echo "$(date +%Y-%m-%d:%H:%M:%S) - semaphore file created" >> $SEMAPHORE_FILE
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"