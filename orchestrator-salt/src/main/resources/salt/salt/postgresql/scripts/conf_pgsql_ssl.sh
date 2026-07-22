#!/usr/bin/env bash

CONFIG_FILE=$(psql -c "show config_file;" -t | xargs)
echo "Config file: $CONFIG_FILE"

set -e

update_directive() {
    local key="$1"
    local value="$2"
    if grep -qE "^${key}\s*=" "${CONFIG_FILE}"; then
        echo "Updating ${key} in postgresql.conf"
        sed -i.orig "/^${key}[[:space:]]*=/c\\${key} = ${value}" "${CONFIG_FILE}"
    else
        echo "Adding ${key} to postgresql.conf"
        echo "${key} = ${value}" >> "${CONFIG_FILE}"
    fi
}

remove_directive() {
    local key="$1"
    if grep -qE "^${key}\s*=" "${CONFIG_FILE}"; then
        echo "Removing ${key} from postgresql.conf"
        sed -i.orig "/^${key}[[:space:]]*=/d" "${CONFIG_FILE}"
    fi
}

update_directive "ssl" "on"
update_directive "ssl_cert_file" "'{{ postgres_directory }}/certs/postgres.cert'"
update_directive "ssl_key_file" "'{{ postgres_directory }}/certs/postgres.key'"

TLS_ADVANCED_CONTROL="{{ tls_advanced_control | default('false') }}"
TLS_MIN_VERSION="{{ tls_min_version | default('') }}"
TLS_MAX_VERSION="{{ tls_max_version | default('') }}"
TLS12_CIPHERS="{{ tls12_ciphers | default('') }}"

[ "${TLS_ADVANCED_CONTROL}" = "None" ] && TLS_ADVANCED_CONTROL=""
[ "${TLS_MIN_VERSION}" = "None" ] && TLS_MIN_VERSION=""
[ "${TLS_MAX_VERSION}" = "None" ] && TLS_MAX_VERSION=""
[ "${TLS12_CIPHERS}" = "None" ] && TLS12_CIPHERS=""

DEFAULT_CIPHERS="ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-SHA256"

if [ "${TLS_ADVANCED_CONTROL}" = "True" ] || [ "${TLS_ADVANCED_CONTROL}" = "true" ]; then
    if [ -n "${TLS12_CIPHERS}" ]; then
        update_directive "ssl_ciphers" "'${TLS12_CIPHERS}'"
    else
        remove_directive "ssl_ciphers"
    fi

    update_directive "ssl_prefer_server_ciphers" "on"

    if [ -n "${TLS_MIN_VERSION}" ]; then
        update_directive "ssl_min_protocol_version" "'${TLS_MIN_VERSION}'"
    else
        remove_directive "ssl_min_protocol_version"
    fi

    if [ -n "${TLS_MAX_VERSION}" ]; then
        update_directive "ssl_max_protocol_version" "'${TLS_MAX_VERSION}'"
    else
        remove_directive "ssl_max_protocol_version"
    fi
else
    update_directive "ssl_ciphers" "'${DEFAULT_CIPHERS}'"
    remove_directive "ssl_prefer_server_ciphers"
    remove_directive "ssl_min_protocol_version"
    remove_directive "ssl_max_protocol_version"
fi
set +e
