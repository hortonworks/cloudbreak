install_adtrust:
  cmd.run:
    - name: |
        function cleanup() {
          kdestroy
        }

        trap cleanup EXIT

        echo $FPW | kinit {{ salt['pillar.get']('sssd-ipa:principal') }}

        ipa-adtrust-install -a ${FPW} -U | tee -a /var/log/ipa-adtrust-install.log
        exit ${PIPESTATUS[0]}
    - env:
        - FPW: {{salt['pillar.get']('freeipa:password')}}
    - failhard: True