#!/usr/bin/env bash

# -e " Exit immediately if a command exits with a non-zero status.
# -x  Print commands and their arguments as they are executed.

: ${CLEANUP:=false}

tmp-cleanup() {
    declare desc="Remove old temporary files and test results before new test run"

    if [[ $CLEANUP == "true" ]]; then
        rm -rf {test-logs,allure,.dp,tmp,test-results}
    fi
}

tmp-create() {
    declare desc="Copy all the files that needs to be modified by local Docker IP"

    mkdir -p tmp
    cp -r scripts/{docker-test.sh,cbm.sh} {docker-compose.yml,swagger-*.json} {certs,responses} tmp
}

main() {
    tmp-cleanup
    tmp-create
}

main "$@"

