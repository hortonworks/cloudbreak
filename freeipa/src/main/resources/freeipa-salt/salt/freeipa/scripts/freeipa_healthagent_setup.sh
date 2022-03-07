#!/usr/bin/env bash
# Name: freeipa_healthagent_setup
# Description: Setup the environment for the freeipa health agent to be used
################################################################
set -x
set -e

#
# Setup cert copy and service refstart on cert update
#
ipa-getcert start-tracking -n Server-Cert -d /etc/httpd/alias -C "/usr/libexec/ipa/certmonger/restart_httpd;/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh"
