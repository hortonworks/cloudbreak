#!/usr/bin/env bash
# Name: freeipa_healthagent_setup
# Description: Setup the environment for the freeipa health agent to be used
################################################################
set -x
set -e

LOG_FILE=/var/log/freeipa-healthagent-setup.log

log() {
    echo $(date) $* >> $LOG_FILE
}

#
# Setup cert copy and service restart on cert update
#
if [ -f "/etc/httpd/alias/cert8.db" ]; then
  log "Start Server-Cert tracking by ipa-getcert"
  ipa-getcert start-tracking -n Server-Cert -d /etc/httpd/alias -C "/usr/libexec/ipa/certmonger/restart_httpd;/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh"
elif [ -f "/var/lib/ipa/certs/httpd.crt" ]; then
  log "Start httpd.crt tracking by systemd"
  systemctl daemon-reload
  systemctl enable httpd-crt-change-tracker
  systemctl start httpd-crt-change-tracker
else
  log "Unable to track any kind of httpd certificate."
  exit 1
fi
