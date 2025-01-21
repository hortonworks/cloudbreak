{%- set postgres_fqdn = salt['grains.get']('fqdn') %}
{%- set postgres_host = salt['grains.get']('host') %}
{%- set cm_keytab = salt['pillar.get']('keytab:CM') %}
#!/usr/bin/env bash
set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

CM_KEYTAB_FILE={{ cm_keytab.path }}
CM_PRINCIPAL={{ cm_keytab.principal }}
CERTS_DIR={{ postgres_directory }}/certs

if [ -e ${CERTS_DIR}/old_cert_sn ]
then
  kinit -kt ${CM_KEYTAB_FILE} ${CM_PRINCIPAL}
  echo "$(date '+%d/%m/%Y %H:%M:%S') - Revoking cert with following Serial number:"
  cat ${CERTS_DIR}/old_cert_sn
  cat ${CERTS_DIR}/old_cert_sn | xargs ipa cert-revoke
fi

rm -f ${CERTS_DIR}/postgres_bkp.key
rm -f ${CERTS_DIR}/postgres_bkp.cert
rm -f ${CERTS_DIR}/old_cert_sn
rm -f ${CERTS_DIR}/new_cert_sn

set +e
