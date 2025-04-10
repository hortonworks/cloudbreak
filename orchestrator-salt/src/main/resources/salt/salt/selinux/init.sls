{% set selinux_mode = salt['pillar.get']('cluster:selinux_mode', 'permissive') %}
{% set selinux_current_state = salt.cmd.run("getenforce | grep 'Disabled' || echo ''") %}
{% set platform = salt['pillar.get']('platform', '') %}
{% do salt.log.debug("log_selinux_mode " ~ selinux_mode) %}
{% do salt.log.debug("log_selinux_current_state " ~ selinux_current_state) %}
{% do salt.log.debug("log_platform " ~ platform) %}

{% if platform != 'YARN' and selinux_current_state != 'Disabled' %}
set_selinux_mode:
  selinux.mode:
    - name: {{ selinux_mode }}

set_httpd_permissive:
  cmd.run:
    - name: semanage permissive -a httpd_t
    - unless: semanage permissive -l | grep httpd_t
{% endif %}