{% set proxy_protocol = salt['pillar.get']('proxy:protocol') %}
{% set proxy_host = salt['pillar.get']('proxy:host') %}
{% set proxy_port = salt['pillar.get']('proxy:port') %}
{% set proxy_user = salt['pillar.get']('proxy:user') %}
{% set proxy_password = salt['pillar.get']('proxy:password') %}
{% set proxy_no_proxy_hosts = salt['pillar.get']('proxy:noProxyHosts') %}
{% set tunnel = salt['pillar.get']('proxy:tunnel') %}

{% set proxy_url = proxy_protocol ~ '://' ~ proxy_host ~ ':' ~ proxy_port %}
{% set proxy_command = 'ProxyCommand /usr/bin/corkscrew ' ~ proxy_host ~ ' ' ~ proxy_port ~ ' %h %p' %}

{% if proxy_user %}
  {% set proxy_url = proxy_protocol ~ '://' ~ proxy_user ~ ':' ~ proxy_password ~ '@' ~ proxy_host ~ ':' ~ proxy_port %}
  {% set proxy_command = proxy_command ~ ' /root/.ssh/proxy_auth' %}
{% endif %}

{% set proxy = {} %}
{% do proxy.update({
  'protocol': proxy_protocol,
  'host': proxy_host,
  'port': proxy_port,
  'user': proxy_user,
  'password': proxy_password,
  'no_proxy_hosts': proxy_no_proxy_hosts,
  'tunnel': tunnel,
  'proxy_url': proxy_url,
  'proxy_command': proxy_command,
}) %}
