#!/usr/bin/env bash

: ${USER_LENGTH:="255"}

set -e

echo "$FPW" | kinit admin

if [[ $(ipa config-show | grep "Maximum username length" | awk '{print $4}') -eq "$USER_LENGTH" ]]; then
    echo "Maximum username is already configured to $USER_LENGTH"
else
    ipa config-mod --maxusername="$USER_LENGTH"
fi

kdestroy

set +e