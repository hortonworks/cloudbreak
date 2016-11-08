{% set username = salt['pillar.get']('ambari:username') %}
{% set password = salt['pillar.get']('ambari:password') %}
{% set version = salt['pillar.get']('ambari:repo:version') %}

{% set ambari = {} %}
{% do ambari.update({
    'username' : username,
    'password' : password,
    'version': version,
}) %}