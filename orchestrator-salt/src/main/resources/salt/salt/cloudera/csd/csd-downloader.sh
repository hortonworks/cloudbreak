#!/usr/bin/env bash

set -e

{%- if salt['pillar.get']('proxy:host') %}
proxy={{ pillar['proxy']['protocol'] }}://{{ pillar['proxy']['host'] }}:{{ pillar['proxy']['port'] }}
{%- if salt['pillar.get']('proxy:password') %}
proxy_auth={{ pillar['proxy']['user'] }}:{{ pillar['proxy']['password'] }}
PROXY_FLAG="-x $proxy_auth@$proxy"
{% else %}
PROXY_FLAG="-x $proxy"
{%- endif %}
{%- endif %}

{% if salt['pillar.get']('cloudera-manager:csd-urls') %}
csdUrls=({%- for url in salt['pillar.get']('cloudera-manager:csd-urls') -%}
{{ url + " " }}
{%- endfor %})

{%- if salt['pillar.get']('cloudera-manager:paywall_username') %}
CREDENTIAL="{{ salt['pillar.get']('cloudera-manager:paywall_username') }}:{{ salt['pillar.get']('cloudera-manager:paywall_password') }}"
echo "$(date '+%d/%m/%Y %H:%M:%S') - Paywall credential found " |& tee -a /var/log/csd_downloader.log
{%- endif %}

{% if not salt['pillar.get']('cloudera-manager:upgrade-preparation') %}
rm -rf /opt/cloudera/csd
mkdir -p /opt/cloudera/csd
cd /opt/cloudera/csd
{%- endif %}

for url in ${csdUrls[@]}
do
  fileName=$(basename $url)
  echo "$(date '+%d/%m/%Y %H:%M:%S') - Trying to download CSD file name ($fileName) from URL: ($url) " |& tee -a /var/log/csd_downloader.log
  if test -f $fileName
  then
    echo "$(date '+%d/%m/%Y %H:%M:%S') - ($fileName) already exists " |& tee -a /var/log/csd_downloader.log
  else
    if [[ $url =~ "archive.cloudera.com" ]] && [ $CREDENTIAL ];
    then
      AUTH_FLAG="-u $CREDENTIAL"
      echo "$(date '+%d/%m/%Y %H:%M:%S') - Adding paywall credential for authentication header ($url) " |& tee -a /var/log/csd_downloader.log
    else
      echo "$(date '+%d/%m/%Y %H:%M:%S') - Paywall credential is not necessary to access CSD ($url) " |& tee -a /var/log/csd_downloader.log
    fi
    echo "$(date '+%d/%m/%Y %H:%M:%S') - Downloading ($url) " |& tee -a /var/log/csd_downloader.log
    curl -L -O -R --fail $PROXY_FLAG $AUTH_FLAG $url |& tee -a /var/log/csd_downloader.log
  fi
done
{% else %}
echo "$(date '+%d/%m/%Y %H:%M:%S') - No CSDs to download. " |& tee -a /var/log/csd_downloader.log

{% endif %}
