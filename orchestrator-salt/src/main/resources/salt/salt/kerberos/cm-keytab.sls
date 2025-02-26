{% if "ipa_member" in grains.get('roles', []) and pillar['keytab'] is defined and pillar['keytab']['CM'] is defined and salt['pillar.get']('keytab:CM') != None %}

set_cm_keytab_permission:
  file.managed:
    - replace: False
    - name: {{salt['pillar.get']('keytab:CM:path')}}
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600

configure_cm_principal:
  file.managed:
    - name: /etc/cloudera-scm-server/cmf.principal
    - user: cloudera-scm
    - group: cloudera-scm
    - contents_pillar: keytab:CM:principal

{% endif %}


{% if "ad_member" in grains.get('roles', []) %}
{% from 'sssd/ad-settings.sls' import ad with context %}

configure_cm_principal_ad:
  file.managed:
    - name: /etc/cloudera-scm-server/cmf.principal
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600
    - contents: {{ ad.server_hostname }}$

copy_keytab_cm:
  file.copy:
    - name: /etc/cloudera-scm-server/cmf.keytab
    - source: /etc/krb5.keytab
    - user: cloudera-scm
    - group: cloudera-scm
    - mode: 600

{% endif %}