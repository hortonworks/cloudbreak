{% set cloudera_manager_database = salt['pillar.get']('cloudera-manager:database') %}

{% set cloudera_manager = {} %}
{% do cloudera_manager.update({
    'cloudera_manager_database': cloudera_manager_database
}) %}