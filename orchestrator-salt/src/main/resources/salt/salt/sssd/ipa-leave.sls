leave-ipa:
  cmd.run:
    - name: ipa host-del {{ salt['grains.get']('fqdn') }} --updatedns && ipa-client-install --uninstall -U
    - onlyif: ipa env
