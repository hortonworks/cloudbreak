#!/usr/bin/env bash

LOG_FILE=/var/log/httpd-crt-tracking.log

log() {
    echo $(date) $* >> $LOG_FILE
}

log "Start /var/lib/ipa/certs/httpd.crt tracking by inotifywait"
inotifywait -q -m -e close_write /var/lib/ipa/certs/httpd.crt |
    while read -r filename event; do
        log "Change has been detected by inotifywait in $filename. Execute /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh"
        /cdp/ipahealthagent/freeipa_healthagent_getcerts.sh;
    done