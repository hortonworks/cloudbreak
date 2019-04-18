{% set cloudera_manager_database = salt['pillar.get']('cloudera-manager:database') %}
{% set ldap = salt['pillar.get']('ldap') %}

{% set cloudera_manager = {} %}
{% do cloudera_manager.update({
    'cloudera_manager_database': cloudera_manager_database,
    'ldap': ldap
}) %}
