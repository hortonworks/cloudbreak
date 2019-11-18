{% set cloudera_manager_database = salt['pillar.get']('cloudera-manager:database') %}
{% set cm_communication = salt['pillar.get']('cloudera-manager:communication') %}
{% set ldap = salt['pillar.get']('ldap') %}
{% set cm_keytab = salt['pillar.get']('keytab:CM') %}
{% set settings = salt['pillar.get']('cloudera-manager:settings') %}

{% set cloudera_manager = {} %}
{% do cloudera_manager.update({
    'cloudera_manager_database': cloudera_manager_database,
    'ldap': ldap,
    'cm_keytab': cm_keytab,
    'communication': cm_communication,
    'settings': settings
}) %}
