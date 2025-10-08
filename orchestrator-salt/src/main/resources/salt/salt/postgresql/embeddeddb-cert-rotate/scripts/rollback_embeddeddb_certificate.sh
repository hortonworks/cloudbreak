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

kinit -kt ${CM_KEYTAB_FILE} ${CM_PRINCIPAL}

if [ -e ${CERTS_DIR}/new_cert_sn ]
then
  echo "$(date '+%d/%m/%Y %H:%M:%S') - Revoke newly generated certificate with serial number:"
  cat ${CERTS_DIR}/new_cert_sn
  cat ${CERTS_DIR}/new_cert_sn | xargs ipa cert-revoke
fi

if [ -e ${CERTS_DIR}/old_cert_sn ]
then
  echo "$(date '+%d/%m/%Y %H:%M:%S') - Restoring usage of certificate with serial number:"
  cat ${CERTS_DIR}/old_cert_sn
fi

rm -f ${CERTS_DIR}/old_cert_sn
rm -f ${CERTS_DIR}/new_cert_sn

mv -f ${CERTS_DIR}/postgres_bkp.key ${CERTS_DIR}/postgres.key
mv -f ${CERTS_DIR}/postgres_bkp.cert ${CERTS_DIR}/postgres.cert

chown -R postgres:postgres ${CERTS_DIR}
chmod 600 ${CERTS_DIR}/postgres.key
rm -f ${CERTS_DIR}/postgres.csr

systemctl --system daemon-reload
systemctl reload postgresql

set +e
