#!/bin/bash

set -ex

CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
AGENT_HOSTS={%- for ip, args in pillar.get('hosts', {}).items() %}{{ args['fqdn'] }}{{ "," if not loop.last else "" }}{%- endfor %}

source /bin/activate_salt_env

for host in ${AGENT_HOSTS//,/ }
do
  mkdir -p $CERTMANAGER_DIR/$host
  /opt/cloudera/cm-agent/bin/certmanager --location $CERTMANAGER_DIR gen_cert_request_token --output $CERTMANAGER_DIR/$host/cmagent.token --hostname $host --lifetime 3600
  salt-cp "$host" $CERTMANAGER_DIR/$host/cmagent.token /etc/cloudera-scm-agent/cmagent.token
done

