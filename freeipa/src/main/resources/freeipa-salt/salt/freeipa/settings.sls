{% set freeipa = {} %}
     {% set backup_location = salt['pillar.get']('freeipa:backup:location') %}
     {% set backup_platform = salt['pillar.get']('freeipa:backup:platform') %}
     {% set azure_instance_msi = salt['pillar.get']('freeipa:backup:azure_instance_msi') %}
     {% set http_proxy = salt['pillar.get']('freeipa:backup:http_proxy') %}
     {% set hostname = salt['grains.get']('fqdn') %}

     {% do freeipa.update({
         "backup_platform" : backup_platform,
         "backup_location" : backup_location,
         "hostname": hostname,
         "azure_instance_msi": azure_instance_msi,
         "http_proxy": http_proxy
     }) %}
