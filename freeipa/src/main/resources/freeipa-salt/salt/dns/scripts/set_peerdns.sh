#!/usr/bin/env bash

set -x

NETWORK_SCRIPT_DIR=/etc/sysconfig/network-scripts

if [ ! -d "$NETWORK_SCRIPT_DIR" ]; then
  echo "$NETWORK_SCRIPT_DIR doesn't exists"
  exit 0
fi

cd $NETWORK_SCRIPT_DIR

for cfg in `ls ifcfg-* | grep -v "\.bak"`; do
    grep -q "PEERDNS=no" $cfg || echo "PEERDNS=no" >> $cfg
done