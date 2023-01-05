{%- set postgres_fqdn = salt['grains.get']('fqdn') %}
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

kinit -kt $CM_KEYTAB_FILE $CM_PRINCIPAL

mkdir -p ${CERTS_DIR}

openssl req -nodes -newkey rsa:2048 -days 365 -keyout ${CERTS_DIR}/postgres.key -out ${CERTS_DIR}/postgres.csr -subj "/CN={{postgres_fqdn}}"
ipa cert-request ${CERTS_DIR}/postgres.csr --principal=postgres/{{ postgres_fqdn }} --add --certificate-out=${CERTS_DIR}/postgres.cert

chown -R postgres:postgres ${CERTS_DIR}
chmod 600 ${CERTS_DIR}/postgres.key
rm -f ${CERTS_DIR}/postgres.csr

set +e
