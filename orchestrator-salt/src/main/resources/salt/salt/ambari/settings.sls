{% if grains['init'] == 'upstart' %}
    {% set is_systemd = False %}
{% else %}
    {% set is_systemd = True %}
{% endif %}

{% set is_predefined_repo = salt['pillar.get']('ambari:repo:predefined') %}

{% set version = salt['pillar.get']('ambari:repo:version') %}

{% set ambari_database = salt['pillar.get']('ambari:database') %}

{% set ambari = {} %}
{% do ambari.update({
    'is_systemd' : is_systemd,
    'is_predefined_repo' : is_predefined_repo,
    'version': version,
    'ambari_database': ambari_database
}) %}