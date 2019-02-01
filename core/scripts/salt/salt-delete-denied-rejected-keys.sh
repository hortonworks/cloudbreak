#!/usr/bin/env bash
#
# This script reads salt keys on the master.
# If there are denied or rejected keys, then the script attempts to delete them.
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
#   */3 * * * * /<PATH_TO_SCRIPT>/salt_delete_dead_keys.sh >> /var/log/salt_delete_dead_keys.log 2>&1
#
#   This will run the script every 3 minutes and will redirect the output to a log file.
#
# 4. exit vi  => this will save the crontab and let the crontab service read modificaitons you made.
#
# Please check logs regularly. Rolling appending is not implemented.
#
#

SCRIPT_VERSION="V1.0"
SCRIPT_NAME="remove denied or rejected salt keys"

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

function main {
    log "Started $SCRIPT_NAME as '$(whoami)' - $SCRIPT_VERSION"
    log "=============================================================="

    salt_keys=$(salt-key --out json)
    log "Retrieved salt keys: $salt_keys"

    denied_keys=$(echo $salt_keys  | jq -r '.minions_denied | .[]')
    delete_keys "denied" $denied_keys

    rejected_keys=$(echo $salt_keys  | jq -r '.minions_rejected | .[]')
    delete_keys "rejected" $rejected_keys

    log "Finished $SCRIPT_NAME"
    echo
}

main "$@"
