#!/bin/bash

set -ex

CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
AGENT_HOSTS={%- for ip, args in pillar.get('hosts', {}).items() %}{{ args['fqdn'] }}{{ "," if not loop.last else "" }}{%- endfor %}
TOKEN_DIR="/srv/salt/agent-tls-tokens"

source /bin/activate_salt_env

for host in ${AGENT_HOSTS//,/ }
do
  mkdir -p $TOKEN_DIR/$host
  /opt/cloudera/cm-agent/bin/certmanager --location $CERTMANAGER_DIR gen_cert_request_token --output $TOKEN_DIR/$host/cmagent.token --hostname $host --lifetime 3600
done

for host in ${AGENT_HOSTS//,/ }
do
  salt $host cp.get_file salt://agent-tls-tokens/$host/cmagent.token /etc/cloudera-scm-agent/cmagent.token
done
