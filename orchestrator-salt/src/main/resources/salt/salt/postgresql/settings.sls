{% set root_certs_enabled = salt['pillar.get']('postgres_root_certs:ssl_certs') is defined and salt['pillar.get']('postgres_root_certs:ssl_certs')|length > 1 %}
{% set root_certs = salt['pillar.get']('postgres_root_certs:ssl_certs') %}
{% set root_certs_file = salt['pillar.get']('postgres_root_certs:ssl_certs_file_path') %}
{% set ssl_enabled = salt['pillar.get']('postgres_root_certs:ssl_enabled', 'False') == 'true' %}
{% set ssl_restart_required = salt['pillar.get']('postgres_root_certs:ssl_restart_required', 'False') == 'true' %}

{% set postgresql = {} %}
{% do postgresql.update({
    'ssl_enabled': ssl_enabled,
    'root_certs': root_certs,
    'root_certs_file': root_certs_file,
    'root_certs_enabled': root_certs_enabled,
    'ssl_restart_required': ssl_restart_required
}) %}
