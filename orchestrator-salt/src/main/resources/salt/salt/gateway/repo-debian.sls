{%- from 'gateway/settings.sls' import gateway with context %}

{% set os = grains['os'] | lower ~ grains['osmajorrelease'] %}

{% if salt['pillar.get']('hdp:stack:vdf-url') != None %}

install_xmllint:
  pkg.installed:
    - pkgs:
      - libxml2-utils

create_repo_from_vdf:
  cmd.run:
    - name: "/opt/salt/generate-repo-for-os-from-vdf.sh {{gateway.vdf_url}} {{gateway.os_family}} | tee -a /var/log/generate-repo-for-os-from-vdf.log && exit ${PIPESTATUS[0]}"
    - unless: ls /var/log/generate-repo-for-os-from-vdf.log
    - require:
      - file: generate_repo_from_vdf_script
      - pkg: install_xmllint
      
{% else %}

create_hdp_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('hdp:stack:' + os) }} HDP main"
    - file: /etc/apt/sources.list.d/hdp.list

create_hdp_utils_repo:
  pkgrepo.managed:
    - name: "deb {{ salt['pillar.get']('hdp:util:' + os) }} HDP-UTILS main"
    - file: /etc/apt/sources.list.d/hdp-utils.list
{% endif %}