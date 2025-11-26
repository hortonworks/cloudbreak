{%- set postgres_fqdn = salt['grains.get']('fqdn') %}
{%- set postgres_host = salt['grains.get']('host') %}
{%- set cm_keytab = salt['pillar.get']('keytab:CM') %}
{%- set os = salt['grains.get']('os') %}
#!/usr/bin/env bash
set -e -u -o pipefail

function get_non_revoked_serial_numbers_array() {
  local non_revoked_serial_numbers=()

{%- if os == 'RedHat' %}
  mapfile -t non_revoked_serial_numbers < <(ipa cert-find --services=${PGSQL_PRINCIPAL} --status=VALID | grep "Serial number: " | cut -f2- -d: | xargs)
{%- else %}
  local serial_numbers=()
  mapfile -t serial_numbers < <(ipa cert-find --services=${PGSQL_PRINCIPAL} | grep "Serial number: " | cut -f2- -d: | xargs)

  local sn
  for sn in ${serial_numbers[@]}; do
    if ipa cert-show "$sn" | grep -q "Revoked: False"; then
      non_revoked_serial_numbers+=("$sn")
    fi
  done
{%- endif %}

  echo "${non_revoked_serial_numbers[@]}"
}

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
mapfile -t non_revoked_serial_numbers < <(get_non_revoked_serial_numbers_array)
printf "%s\n" ${non_revoked_serial_numbers[@]} > ${CERTS_DIR}/old_cert_sn

mv -f ${CERTS_DIR}/postgres.key ${CERTS_DIR}/postgres_bkp.key
mv -f ${CERTS_DIR}/postgres.cert ${CERTS_DIR}/postgres_bkp.cert

openssl req -nodes -newkey rsa:2048 -keyout ${CERTS_DIR}/postgres.key -out ${CERTS_DIR}/postgres.csr -config <(
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
mapfile -t current_serial_numbers < <(get_non_revoked_serial_numbers_array)
for sn in ${current_serial_numbers[@]}; do
  ipa cert-show "$sn"
done

chown -R postgres:postgres ${CERTS_DIR}
chmod 600 ${CERTS_DIR}/postgres.key
rm -f ${CERTS_DIR}/postgres.csr

systemctl --system daemon-reload
systemctl reload postgresql

set +e
