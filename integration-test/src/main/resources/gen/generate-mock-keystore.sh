#!/usr/bin/env bash

set -e

# Generates a throwaway CA (ca.pem, ca-key.pem) and a server certificate signed by it (server.pem, server-key.pem) into the given folder,
# with the same properties as the certm tool used before: RSA 2048, SHA256, O=<org>, 825 days, serverAuth + clientAuth, SANs from the hosts.
# Usage: generate_ca_and_server_cert <folder> <org> [host...]
generate_ca_and_server_cert() (
  umask 077
  local dir=$1
  local org=$2
  shift 2
  local work
  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT

  local san=""
  local host
  for host in "$@"; do
    if [[ "$host" =~ ^[0-9]+(\.[0-9]+){3}$ || "$host" == *:* ]]; then
      san="${san:+$san,}IP:$host"
    else
      san="${san:+$san,}DNS:$host"
    fi
  done

  cat > "$work/ca_ext.cnf" <<EOF
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,digitalSignature,keyEncipherment,keyAgreement
subjectKeyIdentifier=hash
EOF
  cat > "$work/server_ext.cnf" <<EOF
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment,keyAgreement
extendedKeyUsage=serverAuth,clientAuth
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid
EOF
  if [[ -n "$san" ]]; then
    echo "subjectAltName=$san" >> "$work/server_ext.cnf"
  fi

  mkdir -p "$dir"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dir/ca-key.pem"
  openssl req -new -key "$dir/ca-key.pem" -subj "/O=$org" -out "$work/ca.csr"
  openssl x509 -req -in "$work/ca.csr" -signkey "$dir/ca-key.pem" -sha256 -days 825 \
    -set_serial "0x$(openssl rand -hex 16)" -extfile "$work/ca_ext.cnf" -out "$dir/ca.pem"

  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dir/server-key.pem"
  openssl req -new -key "$dir/server-key.pem" -subj "/O=$org" -out "$work/server.csr"
  openssl x509 -req -in "$work/server.csr" -CA "$dir/ca.pem" -CAkey "$dir/ca-key.pem" -sha256 -days 825 \
    -set_serial "0x$(openssl rand -hex 16)" -extfile "$work/server_ext.cnf" -out "$dir/server.pem"
)

generate_ca_and_server_cert "$(pwd)" gateway localhost 127.0.0.1 '*.cluster.local' test mockserver.default.svc.cluster.local \
  qa-mockserver.default.svc.cluster.local eng-mockserver.default.svc.cluster.local
openssl pkcs12 -export -out server.pkcs12 -in server.pem -inkey server-key.pem -passout pass:secret

keytool -genkey -noprompt -dname "CN=mock_server" -keyalg RSA \
  -alias keystore_mock -keystore keystore_mock -storepass secret -keypass secret

keytool -delete -alias keystore_mock -keystore keystore_mock -storepass secret

keytool -importkeystore \
        -deststorepass secret -destkeypass secret -destkeystore keystore_mock \
        -srckeystore server.pkcs12 -srcstoretype PKCS12 -srcstorepass secret \
        -alias 1


cp server.pem ../cluster.pem
cp keystore_mock ../keystore_server
