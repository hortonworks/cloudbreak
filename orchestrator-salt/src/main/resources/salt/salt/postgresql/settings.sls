{% set root_certs_enabled = salt['pillar.get']('postgres_root_certs:ssl_certs') is defined and salt['pillar.get']('postgres_root_certs:ssl_certs')|length > 1 %}
{% set root_certs = salt['pillar.get']('postgres_root_certs:ssl_certs') %}
{% set root_certs_file = salt['pillar.get']('postgres_root_certs:ssl_certs_file_path') %}
{% set ssl_enabled = salt['pillar.get']('postgres_root_certs:ssl_enabled', 'False') == 'true' %}
{% set ssl_restart_required = salt['pillar.get']('postgres_root_certs:ssl_restart_required', 'False') == 'true' %}
{% set ssl_for_cm_db_natively_supported = salt['pillar.get']('postgres_root_certs:ssl_for_cm_db_natively_supported', 'False') == 'true' %}
{% set ssl_verification_mode = salt['pillar.get']('postgres_root_certs:ssl_verification_mode') %}
{% set tls_advanced_control = salt['pillar.get']('postgres_root_certs:tls_advanced_control', 'false') == 'true' %}
{% set tls_min_version = salt['pillar.get']('postgres_root_certs:tls_min_version', '') or '' %}
{% set tls_max_version = salt['pillar.get']('postgres_root_certs:tls_max_version', '') or '' %}
{% set tls12_ciphers = salt['pillar.get']('postgres_root_certs:tls12_ciphers', '') or '' %}
{% set tls13_ciphers = salt['pillar.get']('postgres_root_certs:tls13_ciphers', '') or '' %}

{% set postgresql = {} %}
{% do postgresql.update({
    'ssl_enabled': ssl_enabled,
    'root_certs': root_certs,
    'root_certs_file': root_certs_file,
    'root_certs_enabled': root_certs_enabled,
    'ssl_restart_required': ssl_restart_required,
    'ssl_for_cm_db_natively_supported': ssl_for_cm_db_natively_supported,
    'ssl_verification_mode': ssl_verification_mode,
    'tls_advanced_control': tls_advanced_control,
    'tls_min_version': tls_min_version,
    'tls_max_version': tls_max_version,
    'tls12_ciphers': tls12_ciphers,
    'tls13_ciphers': tls13_ciphers
}) %}
