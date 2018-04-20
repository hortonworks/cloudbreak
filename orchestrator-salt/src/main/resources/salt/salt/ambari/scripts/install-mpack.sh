#!/bin/bash
set -ex

ARGS=""
{% if mpack.purge %}
ARGS+="--purge"
{% endif %}
{% if mpack.force %}
ARGS+=" --force"
{% endif %}
{% if mpack.purgeList %}
ARGS+=" --purge-list {{ mpack.purgeList|join(',') }}"
{% endif %}
echo yes | ambari-server install-mpack --mpack={{ mpack.mpackUrl }} ${ARGS} --verbose
echo "$(date +%Y-%m-%d:%H:%M:%S) {{ mpack.mpackUrl }}" >> /var/mpack_installed
