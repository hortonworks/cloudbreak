#!/usr/bin/env bash

set -ex

mkdir -p /opt/cloudera/csd
cd /opt/cloudera/csd

{% if salt['pillar.get']('cloudera-manager:csd-urls') %}
{% for url in salt['pillar.get']('cloudera-manager:csd-urls') %}
curl -L -O -R {{ url }}
{% endfor %}
{% else %}
echo "No CSDs to download." >> /var/csd_downloaded
{% endif %}

echo "$(date +%Y-%m-%d:%H:%M:%S)" >> /var/csd_downloaded
