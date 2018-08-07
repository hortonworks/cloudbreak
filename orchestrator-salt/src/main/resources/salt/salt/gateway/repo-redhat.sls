{%- from 'gateway/settings.sls' import gateway with context %}

{% if salt['pillar.get']('hdp:stack:vdf-url') != None %}

create_repo_from_vdf:
  cmd.run:
    - name: "/opt/salt/generate-repo-for-os-from-vdf.sh {{gateway.vdf_url}} {{gateway.os_family}} | tee -a /var/log/generate-repo-for-os-from-vdf.log && exit ${PIPESTATUS[0]}"
    - unless: ls /var/log/generate-repo-for-os-from-vdf.log
    - require:
      - file: generate_repo_from_vdf_script

{% else %}

/etc/yum.repos.d/HDP.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp.repo
    - template: jinja

/etc/yum.repos.d/HDP-UTILS.repo:
  file.managed:
    - replace: False
    - source: salt://gateway/yum/hdp-utils.repo
    - template: jinja

{% endif %}