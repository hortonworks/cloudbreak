haveged:
  pkg.installed: []
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

/etc/krb5.conf:
  file.managed:
    - source: salt://kerberos/config/krb5.conf-existing
    - template: jinja
