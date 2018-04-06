{% if grains['os_family'] == 'RedHat' %}

/etc/yum.repos.d/ambari.repo:
  file.managed:
    - source: salt://ambari/yum/ambari.repo
    - template: jinja

{% elif grains['os_family'] == 'Debian' %}

create_ambari_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('ambari:repo:baseUrl') }} Ambari main"
    - file: /etc/apt/sources.list.d/ambari.list
{% if salt['pillar.get']('ambari:repo:gpgKeyUrl') %}
    - key_url: "{{ salt['pillar.get']('ambari:repo:gpgKeyUrl') }}"
{% endif %}

{% elif grains['os_family'] == 'Suse' %}

/etc/zypp/repos.d/ambari.repo:
  file.managed:
    - source: salt://ambari/yum/ambari.repo
    - template: jinja

{% if salt['file.file_exists' ]('/etc/zypp/services.d/SMT-http_smt-ec2_susecloud_net.service') %}
disable_repo_service_update_on_aws:
  file.replace:
    - name: /etc/zypp/services.d/SMT-http_smt-ec2_susecloud_net.service
    - pattern: "autorefresh=1"
    - repl: "autorefresh=0"
    - ignore_if_missing: True
    - backup: False

set_sles_sdk_priority_in_service_aws:
  file.line:
    - name: /etc/zypp/services.d/SMT-http_smt-ec2_susecloud_net.service
    - mode: insert
    - content: "repo_31_priority=100"
    - after: "repo_31_enabled=1"
    - ignore_if_missing: True
    - backup: False
{% endif %}

{% if salt['file.file_exists' ]('/etc/zypp/services.d/SMT-http_smt-gce_susecloud_net.service') %}
disable_repo_service_update_on_gce:
  file.replace:
    - name: /etc/zypp/services.d/SMT-http_smt-gce_susecloud_net.service
    - pattern: "autorefresh=1"
    - repl: "autorefresh=0"
    - ignore_if_missing: True
    - backup: False

set_sles_sdk_priority_in_service_gce:
  file.line:
    - name: /etc/zypp/services.d/SMT-http_smt-gce_susecloud_net.service
    - mode: insert
    - content: "repo_31_priority=100"
    - after: "repo_31_enabled=1"
    - ignore_if_missing: True
    - backup: False
{% endif %}

{% if salt['file.file_exists' ]('/etc/zypp/services.d/SMT-http_smt-azure_susecloud_net.service') %}
disable_repo_service_update_on_azure:
  file.replace:
    - name: /etc/zypp/services.d/SMT-http_smt-azure_susecloud_net.service
    - pattern: "autorefresh=1"
    - repl: "autorefresh=0"
    - ignore_if_missing: True
    - backup: False

set_sles_sdk_priority_in_service_azure:
  file.line:
    - name: /etc/zypp/services.d/SMT-http_smt-azure_susecloud_net.service
    - mode: insert
    - content: "repo_31_priority=100"
    - after: "repo_31_enabled=1"
    - ignore_if_missing: True
    - backup: False
{% endif %}

set_sles_sdk_priority_in_repo:
  cmd.run:
    - name: zypper mr -p 100 SLE-SDK12-SP3-Pool

import_ambari_repo_key:
  cmd.run:
    - name: zypper --gpg-auto-import-keys refresh

{% endif %}