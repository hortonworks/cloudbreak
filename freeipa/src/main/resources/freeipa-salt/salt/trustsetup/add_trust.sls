{%- set kdc_realm = salt['pillar.get']('freeipa:trust_setup:kdc_realm') %}
{%- set trust_secret = salt['pillar.get']('freeipa:trust_setup:trust_secret') %}

add_trust:
  cmd.run:
    - name: |
        function cleanup() {
          kdestroy
        }

        trap cleanup EXIT

        echo $FPW | kinit {{ salt['pillar.get']('sssd-ipa:principal') }}
        echo $TRUST_SECRET | ipa -vvv trust-add --trust-secret --type=ad --two-way=true {{ kdc_realm }} | tee -a /var/log/ipa-addtrust.log
        exit ${PIPESTATUS[1]}
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
        - TRUST_SECRET: {{salt['pillar.get']('freeipa:trust_setup:trust_secret')}}
    - failhard: True