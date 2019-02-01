#!/usr/bin/env bash
#
# This script pings all minions, and deletes keys of unreachable minions
#
# This script should be run as a cron job in the name of root on the salt master node.
# Before starting cron, please add executable rights to the file:
#    chmod 744 salt_delete_dead_keys.sh
# Please redirect output to a log file (see crontab example below).
#
# To add the script to cron as root, please follow these steps:
# 1. sudo su
# 2. crontab -e   => will start a vi or your default editor, where you can edit crontab.
# 3. add following line:
#  0 0 * * * ? /<PATH_TO_SCRIPT>/salt_remove_dead_minions.sh >> /var/log/salt_remove_dead_minion_keys.log 2>&1
#
#   This will run the script every hour and will redirect the output to a log file.
#
# 4. exit vi  => this will save the crontab and let the crontab service read modificaitons you made.
#
# Please check logs regularly. Rolling appending is not implemented.
#
#

SCRIPT_VERSION="V1.0"
SCRIPT_NAME="remove keys of unreachable salt minions"

function log {
    echo $(date) $@
}

function delete_keys {
    key_category=$1
    shift
    keys_to_delete="$@"
    log "delete $key_category keys: $keys_to_delete"

    if [ -z "$keys_to_delete" ]; then
	log "Currently there are no $key_category keys"
    else
	log "Removing $key_category keys"
	for key in $keys_to_delete; do
	    log "Removing $key_category key $key"
	    key_delete_result=$(salt-key -y -d $key)
	    log "Result: $key_delete_result"
	done
	log "Finished removing $key_category keys"
    fi
}

function log_salt_keys() {
    log_message=$1
    salt_keys=$(salt-key --out json)
    log "$log_message: $salt_keys"
}

function main {
    log "Started $SCRIPT_NAME as '$(whoami)' - $SCRIPT_VERSION"
    log "=============================================================="
    log "pinging minions ..."
    minions=$(salt '*' test.ping --out json )
    unreachable_minions=$(echo "$minions" | grep "Minion did not return" |  awk -F'":' '{print $1}' | sed 's/"//g')
    log "minions: $minions"
    log "unreachable minions: $unreachable_minions"
    log_salt_keys "salt keys before deleting dead minions"
    delete_keys "unreachable" $unreachable_minions
    log_salt_keys "salt keys after deleting dead minions"

    log "Finished script $SCRIPT_NAME"
    echo
}

main "$@"
