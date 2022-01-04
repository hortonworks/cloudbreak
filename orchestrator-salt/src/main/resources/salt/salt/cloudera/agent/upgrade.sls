{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}

include:
  - postgresql.repo
  - cloudera.repo

stop-cloudera-scm-agent:
  service.dead:
    - name: cloudera-scm-agent

{% if grains['os_family'] == 'RedHat' %}

yum_cleanup_all_before_cm_agent_install:
  cmd.run:
    - name: yum clean all

/opt/salt/scripts/check_cmagent_repo_url.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/scripts/check_cm_repo_url.sh.j2
    - template: jinja
    - mode: 700

check_cmagent_repo_url:
  cmd.run:
    - name: /opt/salt/scripts/check_cmagent_repo_url.sh 2>&1 | tee -a /var/log/check_cmagent_repo_url.log && exit ${PIPESTATUS[0]}
    - failhard: True
    - require:
      - file: /opt/salt/scripts/check_cmagent_repo_url.sh

{% endif %}

# .dont_delete files are created as part of the image burning process
# because the parcels that the image contains are not registered in CM
# when it starts for the first time and the scm-agent would just delete
# them. To prevent this deletion we create these files, but after the
# first start it can be deleted as they are registered at that point, but
# when we call the delete parcel API the scm-server doesn't delete them
# until these .dont_delete files are there.

remove_dont_delete_files:
  cmd.run:
    - name: find /opt/cloudera/ -name ".dont_delete" | xargs -I@ rm -f @

# The .flood directory is used by the torrent process, but not cleaned
# up when the parcel distriburion is done. This folder contains all the
# parcels that are distributed so it takes up lots of space on the disk
# unnecessarily.

remove_flood_files:
  file.directory:
    - name: /opt/cloudera/parcels/.flood/
    - user: cloudera-scm
    - group: cloudera-scm
    - dir_mode: 750
    - clean: True

# The parcel-repo folder contains the same parcels as the parcels folder
# along with the torrent file and the hash file. However, it is not used
# at all so during image creation we replace these files with a 0 byte file.
# Unfortunately, when the CM server downloads a new parcel it contains the
# full parcel file, again taking up too much space on the disk.

empty_the_parcel_files_in_parcel_repo:
  cmd.run:
    - name: find /opt/cloudera/parcel-repo -name "*.parcel" | while read file; do :>$file; done

upgrade-cloudera-agent:
  pkg.latest:
    - pkgs:
        - cloudera-manager-agent
        - cloudera-manager-daemons
    - refresh: True
    - fromrepo: cloudera-manager
    - failhard: True
    - require:
        - sls: cloudera.repo

{% if salt['pillar.get']('cloudera-manager:settings:deterministic_uid_gid') == True %}
inituids_dir_exists:
  file.directory:
    - name: /opt/cloudera/cm-agent/service/inituids
    - user: cloudera-scm
    - group: cloudera-scm
    - dir_mode: 750
    - makedirs: True

set_service_uids_migrate:
  cmd.run:
    - name: /opt/cloudera/cm-agent/service/inituids/set-service-uids.py -m -l DEBUG 2>&1 | tee -a /var/log/set-service-uids.log && [[ 0 -eq ${PIPESTATUS[0]} ]] && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/set-service-uids-executed || exit ${PIPESTATUS[0]}
    - cwd: /opt/cloudera/cm-agent/service/inituids
    - failhard: True
    - onlyif: test -f /opt/cloudera/cm-agent/service/inituids/set-service-uids.py
    - unless: test -f /var/log/set-service-uids-executed
    - require:
        - file: inituids_dir_exists
{% endif %}
