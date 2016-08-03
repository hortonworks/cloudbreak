{% set name = salt['pillar.get']('ldap:name') %}
{% set description = salt['pillar.get']('ldap:description') %}
{% set serverHost = salt['pillar.get']('ldap:serverHost') %}
{% set serverPort = salt['pillar.get']('ldap:serverPort') %}
{% set serverSSL = salt['pillar.get']('ldap:serverSSL') %}
{% set bindDn = salt['pillar.get']('ldap:bindDn') %}
{% set bindPassword = salt['pillar.get']('ldap:bindPassword') %}
{% set userSearchBase = salt['pillar.get']('ldap:userSearchBase') %}
{% set userSearchFilter = salt['pillar.get']('ldap:userSearchFilter') %}
{% set groupSearchBase = salt['pillar.get']('ldap:groupSearchBase') %}
{% set groupSearchFilter = salt['pillar.get']('ldap:groupSearchFilter') %}
{% set principalRegex = salt['pillar.get']('ldap:principalRegex') %}

{% set ldap = {} %}
{% do ldap.update({
    'name': name,
    'description': description,
    'serverHost': serverHost,
    'serverPort': serverPort,
    'serverSSL': serverSSL,
    'bindDn': bindDn,
    'bindPassword': bindPassword,
    'userSearchBase': userSearchBase,
    'userSearchFilter': userSearchFilter,
    'groupSearchBase': groupSearchBase,
    'groupSearchFilter': groupSearchFilter,
    'principalRegex': principalRegex
}) %}