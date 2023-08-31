create_rotate_ccm_script:
  file.managed:
    - name: /opt/salt/scripts/rotate-ccm.sh
    - user: root
    - group: root
    - mode: 700
    - makedirs: True
    - source: salt://{{ slspath }}/scripts/rotate-ccm.sh

call_rotate_ccm:
  cmd.run:
    - name: /opt/salt/scripts/rotate-ccm.sh 2>&1 | tee -a /var/log/rotate-ccm.log && exit ${PIPESTATUS[0]}
    - runas: root
    - shell: /bin/bash
    - failhard: True
    - require:
        - file: create_rotate_ccm_script
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
        - CCM_V2_AGENT_HMAC_KEY: "{{salt['pillar.get']('ccm_jumpgate:agent_hmac_key')}}"
        - CCM_V2_IV: "{{salt['pillar.get']('ccm_jumpgate:initialisation_vector')}}"
        - CCM_V2_AGENT_HMAC_FOR_PRIVATE_KEY: "{{salt['pillar.get']('ccm_jumpgate:agent_hmac_for_private_key')}}"

include:
  - rotateccm.schedule
