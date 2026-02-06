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

mkdir -p ${CERTS_DIR}

openssl req -nodes -newkey rsa:3072 -days 365 -keyout ${CERTS_DIR}/postgres.key -out ${CERTS_DIR}/postgres.csr -config <(
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

PGSQL_PRINCIPAL=postgres/{{ postgres_fqdn }}
PGSQL_PRINCIPAL_ALIAS=postgres/{{ postgres_host }}

echo "Checking if PgSQL service principal \"${PGSQL_PRINCIPAL}\" exists."
if ipa service-show ${PGSQL_PRINCIPAL} &> /dev/null
then
  echo "Found PgSQL service principal \"${PGSQL_PRINCIPAL}\"."
else
  echo "PgSQL service principal \"${PGSQL_PRINCIPAL}\" is absent. Attempting to create it."
  ipa service-add ${PGSQL_PRINCIPAL}
fi

echo "Checking if PgSQL service principal alias \"${PGSQL_PRINCIPAL_ALIAS}\" exists."
if ipa service-show ${PGSQL_PRINCIPAL} --raw | grep "krbprincipalname:" | grep -q "${PGSQL_PRINCIPAL_ALIAS}@"
then
  echo "Found PgSQL service principal alias \"${PGSQL_PRINCIPAL_ALIAS}\"."
else
  echo "PgSQL service principal alias \"${PGSQL_PRINCIPAL_ALIAS}\" is absent. Attempting to add it."
  ipa service-add-principal ${PGSQL_PRINCIPAL} ${PGSQL_PRINCIPAL_ALIAS}
fi

ipa cert-request ${CERTS_DIR}/postgres.csr --principal=${PGSQL_PRINCIPAL} --certificate-out=${CERTS_DIR}/postgres.cert

chown -R postgres:postgres ${CERTS_DIR}
chmod 600 ${CERTS_DIR}/postgres.key
rm -f ${CERTS_DIR}/postgres.csr

set +e
