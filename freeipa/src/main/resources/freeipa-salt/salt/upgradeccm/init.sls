create_upgrade_ccm_script:
  file.managed:
    - name: /opt/salt/scripts/upgrade-ccm.sh
    - user: root
    - group: root
    - mode: 700
    - makedirs: True
    - source: salt://{{ slspath }}/scripts/upgrade-ccm.sh

call_upgrade_ccm:
  cmd.run:
    - name: /opt/salt/scripts/upgrade-ccm.sh | tee -a /var/log/upgrade-ccm.log
    - runas: root
    - shell: /bin/bash
    - failhard: True
    - require:
        - file: create_upgrade_ccm_script
    - env:
        - CLOUD_PLATFORM: "{{salt['pillar.get']('platform')}}"
        - CCM_V2_INVERTING_PROXY_CERTIFICATE: "{{salt['pillar.get']('ccm_jumpgate:inverting_proxy_certificate')}}"
        - CCM_V2_INVERTING_PROXY_HOST: "{{salt['pillar.get']('ccm_jumpgate:inverting_proxy_host')}}"
        - CCM_V2_AGENT_CERTIFICATE: "{{salt['pillar.get']('ccm_jumpgate:agent_certificate')}}"
        - CCM_V2_AGENT_ENCIPHERED_KEY: "{{salt['pillar.get']('ccm_jumpgate:agent_enciphered_key')}}"
        - CCM_V2_AGENT_KEY_ID: "{{salt['pillar.get']('ccm_jumpgate:agent_key_id')}}"
        - CCM_V2_AGENT_BACKEND_ID_PREFIX: "{{salt['pillar.get']('ccm_jumpgate:agent_backend_id_prefix')}}"
        - ENVIRONMENT_CRN: "{{salt['pillar.get']('ccm_jumpgate:environment_crn')}}"
        - CCM_V2_AGENT_ACCESS_KEY_ID: "{{salt['pillar.get']('ccm_jumpgate:agent_access_key_id')}}"
        - CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY: "{{salt['pillar.get']('ccm_jumpgate:agent_enciphered_access_key')}}"

include:
  - upgradeccm.schedule