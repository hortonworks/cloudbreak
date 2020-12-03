#!/usr/bin/env bash

set -ex

mkdir -p /opt/cloudera/csd
cd /opt/cloudera/csd

{% if salt['pillar.get']('cloudera-manager:csd-urls') %}
csdUrls=({%- for url in salt['pillar.get']('cloudera-manager:csd-urls') -%}
{{ url + " " }}
{%- endfor %})

for url in ${csdUrls[@]}
do
  fileName=$(basename $url)
  echo "$(date '+%d/%m/%Y %H:%M:%S') - CSD file name ($fileName) from URL: ($url) " |& tee -a /var/log/csd_downloader.log
  if test -f $fileName
  then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - ($fileName) already exists " |& tee -a /var/log/csd_downloader.log

  else
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Downloading ($url) " |& tee -a /var/log/csd_downloader.log

    curl -L -O -R $url
  fi
done

{% else %}
echo "No CSDs to download." >> /var/log/csd_downloaded
echo "$(date '+%d/%m/%Y %H:%M:%S') - No CSDs to download. " |& tee -a /var/log/csd_downloader.log

{% endif %}
