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
PGSQL_PRINCIPAL=postgres/{{ postgres_fqdn }}

if [ -e ${CERTS_DIR}/new_cert_sn ]
then
  echo "$(date '+%d/%m/%Y %H:%M:%S') - New certificate already generated, which means this is a retry, rollback should happen soon."
  exit 1
fi

if [ -e ${CERTS_DIR}/postgres.csr ]
then
  echo "$(date '+%d/%m/%Y %H:%M:%S') - New certificate request already generated, which means this is a retry, rollback should happen soon."
  exit 1
fi

kinit -kt ${CM_KEYTAB_FILE} ${CM_PRINCIPAL}
ipa cert-find --services=${PGSQL_PRINCIPAL} --status=VALID | grep "Serial number: " | cut -f2- -d: > ${CERTS_DIR}/old_cert_sn

mv -f ${CERTS_DIR}/postgres.key ${CERTS_DIR}/postgres_bkp.key
mv -f ${CERTS_DIR}/postgres.cert ${CERTS_DIR}/postgres_bkp.cert

openssl req -nodes -newkey rsa:2048 -days 365 -keyout ${CERTS_DIR}/postgres.key -out ${CERTS_DIR}/postgres.csr -config <(
cat <<-EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no
[req_distinguished_name]
CN = {{postgres_host}}
[v3_req]
subjectAltName = DNS:{{postgres_fqdn}}
EOF
)

ipa cert-request ${CERTS_DIR}/postgres.csr --principal=${PGSQL_PRINCIPAL} --certificate-out=${CERTS_DIR}/postgres.cert | grep "Serial number: " | cut -f2- -d: > ${CERTS_DIR}/new_cert_sn

echo "$(date '+%d/%m/%Y %H:%M:%S') - Current certs for postgres"
ipa cert-find --services=${PGSQL_PRINCIPAL} --status=VALID

chown -R postgres:postgres ${CERTS_DIR}
chmod 600 ${CERTS_DIR}/postgres.key
rm -f ${CERTS_DIR}/postgres.csr

systemctl --system daemon-reload
systemctl restart postgresql

set +e
