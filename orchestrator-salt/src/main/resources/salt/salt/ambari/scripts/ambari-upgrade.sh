#!/bin/bash

set -x

upgrade_ambari_db() {
    ambari-server upgrade -s
}

main() {
  if ! cat /var/ambari-upgrade.history | grep '{{ ambari.version }}' &>/dev/null; then
    upgrade_ambari_db
    echo '{{ ambari.version }}' >> /var/ambari-upgrade.history
  fi
}

main "$@"