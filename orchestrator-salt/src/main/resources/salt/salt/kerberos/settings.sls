{% set master_key = salt['pillar.get']('kerberos:masterKey') %}
{% set realm = salt['pillar.get']('kerberos:realm') %}
{% set password = salt['pillar.get']('kerberos:password') %}
{% set user = salt['pillar.get']('kerberos:user') %}

{% set kerberos = {} %}
{% do kerberos.update({
    'master_key': master_key,
    'realm': realm,
    'password': password,
    'user': user
}) %}