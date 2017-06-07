{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{%- set server_address = salt['mine.get']('G@roles:ambari_server', 'network.ipaddrs', expr_form = 'compound').values()[0][0] %}

{% set is_predefined_repo = salt['pillar.get']('ambari:repo:predefined') %}

{% set is_container_executor = salt['pillar.get']('docker:enableContainerExecutor') %}

{% set version = salt['pillar.get']('ambari:repo:version') %}

{% set ambari_database = salt['pillar.get']('ambari:database') %}

{%- set cluster_domain = salt['pillar.get']('hosts')[salt['network.interface_ip']('eth0')]['domain'] %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'server_address' : server_address,
    'is_predefined_repo' : is_predefined_repo,
    'version': version,
    'ambari_database': ambari_database,
    'cluster_domain': cluster_domain,
    'is_container_executor': is_container_executor
}) %}