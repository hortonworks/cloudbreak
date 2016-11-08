#!/bin/bash

set -x

upgrade() {
    # temp workaround to automate the process
    cp /usr/lib/python2.7/site-packages/hst_agent/upgrade/UpgradeService120.py /usr/lib/python2.7/site-packages/hst_agent/upgrade/UpgradeService120.py.bak
    sed -i 's/^ *UpgradeService120.AMBARI_PASSWORD.*/            UpgradeService120.AMBARI_PASSWORD="{{ ambari.password }}"/' /usr/lib/python2.7/site-packages/hst_agent/upgrade/UpgradeService120.py

    hst upgrade-ambari-service <<EOF
localhost
8080
{{ ambari.username }}
EOF

   # restore the original file
   cp -f /usr/lib/python2.7/site-packages/hst_agent/upgrade/UpgradeService120.py.bak /usr/lib/python2.7/site-packages/hst_agent/upgrade/UpgradeService120.py
}

main() {
  if ! cat /var/smartsense-upgrade.history | grep '{{ ambari.version }}' &>/dev/null; then
    upgrade
    echo '{{ ambari.version }}' >> /var/smartsense-upgrade.history
  fi
}

main "$@"