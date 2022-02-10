{% set server_address = salt['pillar.get']('cloudera-manager:address') %}
{% set platform = salt['pillar.get']('platform') %}
{% set clusterName = salt['pillar.get']('cluster:name') %}
{% set cluster_domain = salt['pillar.get']('hosts')[server_address]['domain'] %}
{% set cluster_in_childenvironment = salt['pillar.get']('cluster:deployedInChildEnvironment') %}



{% set metadata = {} %}
{% do metadata.update({
    'cluster_domain' : cluster_domain,
    'server_address' : server_address,
    'platform' : platform,
    'clusterName' : clusterName,
    'cluster_in_childenvironment' : cluster_in_childenvironment
}) %}

{% set hostattrs = salt['pillar.get']('hostattrs:'~grains['fqdn']) %}