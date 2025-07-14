{%- set secretEncryptionEnabled = True if salt['pillar.get']('cluster:secretEncryptionEnabled', False) == True else False %}
{%- set cCacheStoragePath = salt['pillar.get']('kerberos:cCacheSecretLocation') %}
{%- set kerberosConfigOriginalPath = '/etc' %}
{%- set kerberosConfigPath = salt['pillar.get']('kerberos:kerberosSecretLocation') if secretEncryptionEnabled == True else kerberosConfigOriginalPath %}
haveged:
  pkg.installed:
    - unless:
      - rpm -q haveged
  service.running:
    - enable: True

{% if grains['os_family'] == 'Suse' %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
{% elif grains['os_family'] == 'Debian' %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-kdc
      - krb5-admin-server
{% else %}
install_kerberos:
  pkg.installed:
    - pkgs:
      - krb5-server
      - krb5-libs
      - krb5-workstation
    - unless:
      - rpm -q krb5-server krb5-libs krb5-workstation
{% endif %}

{% if grains['os_family'] == 'Suse' %}
/var/kerberos:
  file.symlink:
      - target: /var/lib/kerberos
      - force: True
{% endif %}

{% if grains['os_family'] == 'Debian' %}
/var/kerberos:
  file.symlink:
      - target: /etc
      - force: True
{% endif %}

{% if secretEncryptionEnabled == True %}
{{ cCacheStoragePath }}:
  file.directory:
    - user: root
    - group: root
    - mode: 777
    - makedirs: True
copy_krb5_keytab:
  file.copy:
    - name: {{ kerberosConfigPath }}/krb5.keytab
    - source: {{ kerberosConfigOriginalPath }}/krb5.keytab
    - onlyif: test ! -h {{ kerberosConfigOriginalPath }}/krb5.keytab
{{ kerberosConfigOriginalPath }}/krb5.keytab:
  file.symlink:
    - target: {{ kerberosConfigPath }}/krb5.keytab
    - force: True
{% endif %}

/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf-existing
    - template: jinja
    {% if secretEncryptionEnabled == True %}
    - require:
      - file: {{ cCacheStoragePath }}
      - file: {{ kerberosConfigPath }}/krb5.keytab
    {% endif %}

{% if pillar['trust'] is defined and pillar['trust']['realm'] is defined %}
/etc/krb5.conf.d/trust.conf:
  file.managed:
    - source: salt://kerberos/config/trust.conf.j2
    - template: jinja
{% endif %}

{%- if "manager_server" in grains.get('roles', []) %}

add_default_realm_to_cm:
  file.append:
    - name: /etc/cloudera-scm-server/cm.settings
    - makedirs: True
    - template: jinja
    - source: salt://kerberos/config/cm-krb.j2
    - unless: grep "SECURITY_REALM" /etc/cloudera-scm-server/cm.settings

{%- endif %}