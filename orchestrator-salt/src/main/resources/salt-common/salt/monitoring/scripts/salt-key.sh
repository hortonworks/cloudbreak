#!/bin/bash

/opt/salt_*/bin/salt-key --list-all --out=json --out-file=/tmp/salt_keys.txt
result=$?
if [[ "$result" == "0" && -f /tmp/salt_keys.txt ]]; then
  minions=$(jq '.minions | length' /tmp/salt_keys.txt)
  minions_rejected=$(jq '.minions_rejected | length' /tmp/salt_keys.txt)
  minions_unaccepted=$(jq '.minions_pre | length' /tmp/salt_keys.txt)
  minions_denied=$(jq '.minions_denied | length' /tmp/salt_keys.txt)
  echo "cb_salt_minion_accepted $minions" > /var/lib/node_exporter/files/salt_keys.prom.$$
  echo "cb_salt_minion_unaccepted $minions_unaccepted" >> /var/lib/node_exporter/files/salt_keys.prom.$$
  echo "cb_salt_minion_rejected $minions_rejected" >> /var/lib/node_exporter/files/salt_keys.prom.$$
  echo "cb_salt_minion_denied $minions_denied" >> /var/lib/node_exporter/files/salt_keys.prom.$$
  mv /var/lib/node_exporter/files/salt_keys.prom.$$ /var/lib/node_exporter/files/salt_keys.prom
  rm -rf /tmp/salt_keys.txt
fi