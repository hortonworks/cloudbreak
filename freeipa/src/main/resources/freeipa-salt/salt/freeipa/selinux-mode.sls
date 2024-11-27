{% set selinux_mode = salt['pillar.get']('freeipa:selinux_mode', 'permissive') %}
{% set selinux_current_state = salt.cmd.run("getenforce | grep 'Disabled' || echo ''") %}
{% set platform = salt['pillar.get']('platform') %}

{%- do salt.log.debug("log_selinux_mode " ~ selinux_mode) %}
{%- do salt.log.debug("log_selinux_current_state " ~ selinux_current_state) %}
{%- do salt.log.debug("log_platform " ~ platform) %}

{% if platform != 'YARN' and selinux_current_state != 'Disabled' and selinux_mode == 'enforcing' %}
set_selinux_mode:
  selinux.mode:
    - name: {{ selinux_mode }}

/opt/salt/scripts/selinux-add-allowed-ports.sh:
  file.managed:
    - source: salt://freeipa/scripts/selinux-add-allowed-ports.j2
    - template: jinja
    - makedirs: True
    - user: root
    - group: root
    - mode: 755

configure_1080_selinux_port:
  selinux.port_policy_present:
    - name: http_port_t
    - port: 1080
    - protocol: tcp
    - sel_type: http_port_t

configure_9443_selinux_port:
  selinux.port_policy_present:
    - name: http_port_t
    - port: 9443
    - protocol: tcp
    - sel_type: http_port_t

configure_3080_selinux_port:
  selinux.port_policy_present:
    - name: http_port_t
    - port: 3080
    - protocol: tcp
    - sel_type: http_port_t

configure_7070_selinux_port:
  selinux.port_policy_present:
    - name: http_port_t
    - port: 7070
    - protocol: tcp
    - sel_type: http_port_t

create_httpd_script_context_log_filter:
  selinux.fcontext_policy_present:
    - name: '/etc/httpd/conf/httpd-log-filter.sh'
    - sel_type: httpd_sys_script_exec_t

create_httpd_script_context_crt_tracking_httpd_sys_script_exec_t:
  selinux.fcontext_policy_present:
    - name: '/cdp/ipahealthagent/httpd-crt-tracking.sh'
    - sel_type: httpd_sys_script_exec_t

create_httpd_script_context_crt_tracking_initrc_exec_t:
  selinux.fcontext_policy_present:
    - name: '/cdp/ipahealthagent/httpd-crt-tracking.sh'
    - sel_type: initrc_exec_t

execute_selinux_allowed_ports:
  cmd.run:
    - name: /opt/salt/scripts/selinux-add-allowed-ports.sh 2>&1 | tee -a /var/log/selinux-add-allowed-ports-allout.log && exit ${PIPESTATUS[0]}
{% endif %}