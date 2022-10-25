{% if salt['pillar.get']('proxy') != None %}
# Proxy added or modified

{%- from 'modifyproxy/settings.sls' import proxy with context %}

{% if proxy.tunnel == 'CCM' %}

{% if proxy.user %}

ccm_ssh_proxy_auth:
  file.managed:
    - name: /root/.ssh/proxy_auth
    - create: True
    - mode: 600
    - contents:
      - "{{ proxy.user }}:{{ proxy.password }}"

{% endif %} # proxy_user

ccm_setup_ssh_proxy:
  file.managed:
    - name: /root/.ssh/config
    - create: True
    - contents:
      - "{{ proxy.proxy_command }}"

{% elif proxy.tunnel == 'CCMV2' or proxy.tunnel == 'CCMV2_JUMPGATE' %}

ccmv2_setup_jumpgate_proxy:
  file.replace:
    - name: /etc/jumpgate/config.toml
    - pattern: "^http_proxy.*"
    - repl: 'http_proxy = "{{ proxy.proxy_url }}"'
    - append_if_not_found: True

{% endif %} # tunnel

proxy_env:
  file.managed:
    - name: /etc/cdp/proxy.env
    - mode: 640
    - makedirs: True
    - contents:
      - "https_proxy={{ proxy.proxy_url }}"
{% if proxy.no_proxy_hosts %}
      - "no_proxy={{ proxy.no_proxy_hosts }}"
{% endif %}

{% else %}
# The proxy is removed

ccm_remove_ssh_proxy_auth:
  file.absent:
    - name: /root/.ssh/proxy_auth

ccm_remove_ssh_proxy:
  file.absent:
    - name: /root/.ssh/config

ccmv2_remove_jumpgate_proxy:
  file.replace:
    - name: /etc/jumpgate/config.toml
    - pattern: "^http_proxy.*"
    - repl: 'http_proxy = ""'
    - ignore_if_missing: True

remove_proxy_env:
  file.absent:
    - name: /etc/cdp/proxy.env

{% endif %} # proxy added/modified/removed

restart_ccm_gateway_tunnel_service:
  cmd.run:
    - name: systemctl restart ccm-tunnel@GATEWAY
    - onlyif: systemctl is-active ccm-tunnel@GATEWAY

restart_ccm_knox_tunnel_service:
  cmd.run:
    - name: systemctl restart ccm-tunnel@KNOX
    - onlyif: systemctl is-active ccm-tunnel@KNOX

restart_ccmv2_jumpgate_service:
  cmd.run:
    - name: systemctl restart jumpgate-agent
    - onlyif: systemctl is-active jumpgate-agent
