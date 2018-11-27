#!/usr/bin/env bash

# -e " Exit immediately if a command exits with a non-zero status.
# -x  Print commands and their arguments as they are executed.

: ${ORIGINAL_IP:=127.0.0.1}
: ${NEW_IP:=192.168.99.100}
: ${CLEANUP:=false}

tmp-cleanup() {
    declare desc="Remove old temporary files and test results before new test run"

    if [[ $CLEANUP == "true" ]]; then
        rm -rf {test_log,allure,.dp,tmp} {test-result.html,test-result.xml}
    fi
}

tmp-create() {
    declare desc="Copy all the files that needs to be modified by local Docker IP"

    mkdir -p tmp
    cp -r scripts/{docker-test.sh,cbm.sh} {docker-compose.yml,swagger.json,uaa.yml} {certs,responses} tmp
}

ip-replace() {
    declare desc="Replace the default Mock IP to the local Docker IP"

    if [[ $ORIGINAL_IP != $NEW_IP ]]; then
        find tmp -type f -exec sed -i "s/$ORIGINAL_IP/$NEW_IP/g" {} +
    fi
}

main() {
    tmp-cleanup
    tmp-create
    ip-replace
}

main "$@"

