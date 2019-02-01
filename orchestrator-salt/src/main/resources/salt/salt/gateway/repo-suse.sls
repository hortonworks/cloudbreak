{%- from 'gateway/settings.sls' import gateway with context %}

{% if salt['pillar.get']('hdp:stack:vdf-url') != None %}

install_xmllint:
  pkg.installed:
    - name: libxml2-tools

create_repo_from_vdf:
  cmd.run:
    - name: "/opt/salt/generate-repo-for-os-from-vdf.sh {{gateway.vdf_url}} {{gateway.os_family}} | tee -a /var/log/generate-repo-for-os-from-vdf.log && exit ${PIPESTATUS[0]}"
    - unless: ls /var/log/generate-repo-for-os-from-vdf.log
    - require:
      - file: generate_repo_from_vdf_script

{% else %}

HDP:
  pkgrepo.managed:
    - humanname: {{ salt['pillar.get']('hdp:stack:repoid') }}
    - baseurl: "{{ salt['pillar.get']('hdp:stack:sles12') }}"
    - gpgcheck: 0
    - enabled: 1
    - path: /


HDP-UTILS:
  pkgrepo.managed:
    - humanname: {{ salt['pillar.get']('hdp:util:repoid') }}
    - baseurl: "{{ salt['pillar.get']('hdp:util:sles12') }}"
    - gpgcheck: 0
    - enabled: 1
    - path: /

{% endif %}