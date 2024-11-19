{% set freeipa = {} %}
     {% set backup_location = salt['pillar.get']('freeipa:backup:location') %}
     {% set backup_platform = salt['pillar.get']('freeipa:backup:platform') %}
     {% set azure_instance_msi = salt['pillar.get']('freeipa:backup:azure_instance_msi') %}
     {% set http_proxy = salt['pillar.get']('freeipa:backup:http_proxy') %}
     {% set hostname = salt['grains.get']('fqdn') %}
     {% set aws_region = salt['pillar.get']('freeipa:backup:aws_region') %}
     {% set aws_endpoint = salt['pillar.get']('freeipa:backup:aws_endpoint') %}
     {% set gcp_service_account = salt['pillar.get']('freeipa:backup:gcp_service_account') %}
     {% set selinux_mode = salt['pillar.get']('freeipa:selinux_mode') %}

     {% do freeipa.update({
         "backup_platform" : backup_platform,
         "backup_location" : backup_location,
         "hostname": hostname,
         "azure_instance_msi": azure_instance_msi,
         "gcp_service_account": gcp_service_account,
         "http_proxy": http_proxy,
         "aws_region": aws_region,
         "aws_endpoint": aws_endpoint,
         "selinux_mode": selinux_mode
     }) %}
