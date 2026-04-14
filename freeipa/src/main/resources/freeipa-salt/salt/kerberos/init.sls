{%- set gov_cloud = salt['pillar.get']('gov_cloud', False) %}
{% if gov_cloud %}
fips_krb_conf:
  file.managed:
    - name: /etc/krb5.conf.d/fips.conf
    - source: salt://kerberos/templates/fips.conf
    - makedirs: True
    - user: root
    - group: root
    - mode: 644
{% endif %}