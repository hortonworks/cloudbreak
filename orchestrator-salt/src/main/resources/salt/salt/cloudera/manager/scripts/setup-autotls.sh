{%- set manager_server_fqdn = salt['pillar.get']('hosts')[server_address]['fqdn'] %}
#!/bin/bash

set -ex

HOSTNAME={{ manager_server_fqdn }}
CM_KEYTAB_FILE={{ cm_keytab.path }}
CM_PRINCIPAL={{ cm_keytab.principal }}
CERTMANAGER_DIR="/etc/cloudera-scm-server/certs"
CERTMANAGER_ARGS=
OUT_FILE=`mktemp -t signed_ca_chain.XXXXXX.pem`

rm -rf $CERTMANAGER_DIR

source /bin/activate_salt_env

kinit -kt $CM_KEYTAB_FILE $CM_PRINCIPAL

/opt/cloudera/cm-agent/bin/certmanager --location $CERTMANAGER_DIR setup --configure-services $CERTMANAGER_ARGS --override ca_dn="CN=${HOSTNAME}" --stop-at-csr --altname DNS:${HOSTNAME}
/opt/cloudera/cm/bin/generate_intermediate_ca_ipa.sh $CM_PRINCIPAL ${CERTMANAGER_DIR}/CMCA/private/ca_csr.pem $OUT_FILE
/opt/cloudera/cm-agent/bin/certmanager --location $CERTMANAGER_DIR setup --configure-services $CERTMANAGER_ARGS --override ca_dn="CN=${HOSTNAME}" --signed-ca-cert=$OUT_FILE --skip-cm-init --altname DNS:${HOSTNAME} > $CERTMANAGER_DIR/auto-tls.init.txt

rm -rf $OUT_FILE

echo "# Auto-tls related configurations" >> /etc/cloudera-scm-server/cm.settings
cat $CERTMANAGER_DIR/auto-tls.init.txt >> /etc/cloudera-scm-server/cm.settings

echo "Auto-TLS initialization completed successfully."

kdestroy

echo $(date +%Y-%m-%d:%H:%M:%S) >> $CERTMANAGER_DIR/autotls_setup_success
