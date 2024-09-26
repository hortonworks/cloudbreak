{% if grains['os_family'] == 'RedHat' %}

{% set cm_repo_file_path = '/etc/yum.repos.d/cloudera-manager-' + salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') + '-' + salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') + '.repo' %}

{% if not salt['file.file_exists'](cm_repo_file_path) %}
remove_old_preparation_repo_files:
  cmd.run:
    - name: find /etc/yum.repos.d/ -type f -name "cloudera-manager-*.repo*" | xargs -I@ rm -f @

yum_cleanup_all_before_cm_package_download:
  cmd.run:
    - name: yum clean all
{%- endif %}

add-cluster-manager-repo:
  pkgrepo.managed:
    - humanname: Cloudera Manager {{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}
    - name: cloudera-manager-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}
    - baseurl: {{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:baseUrl') }}
    - gpgkey: {{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:gpgKeyUrl') }}
    - gpgcheck: 1
{%- if salt['pillar.get']('cloudera-manager:paywall_username') and 'archive.cloudera.com' in salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:baseUrl')%}
    - username: {{ salt['pillar.get']('cloudera-manager:paywall_username') }}
    - password: {{ salt['pillar.get']('cloudera-manager:paywall_password') }}
{%- endif %}

download-cluster-manager-packages:
  pkg.downloaded:
    - pkgs:
        - cloudera-manager-agent: '{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}.el{{ grains['osmajorrelease'] }}'
        - cloudera-manager-server: '{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}.el{{ grains['osmajorrelease'] }}'
        - cloudera-manager-daemons: '{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}.el{{ grains['osmajorrelease'] }}'
    - fromrepo: cloudera-manager-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:version') }}-{{ salt['pillar.get']('cloudera-manager-upgrade-prepare:repo:buildNumber') }}
    - failhard: True

disable-cluster-manager-preparation-repo:
  file.replace:
    - name: {{ cm_repo_file_path }}
    - pattern: "enabled=1"
    - repl: "enabled=0"
    - require:
        - pkg: download-cluster-manager-packages

{% endif %}