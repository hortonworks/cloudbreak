#!/bin/bash
set -x

get_ip() {
  ifconfig eth0 | awk '/inet addr/{print substr($2,6)}'
}

fix_hostname() {
  if grep -q $(get_ip) /etc/hosts ;then
    sed -i "/$(get_ip)/d" /etc/hosts
  else
    echo OK
  fi
}

configure_docker() {
  rm -rf /etc/docker/key.json
  sed -i "/other_args=/d" /etc/sysconfig/docker
  sh -c ' echo DOCKER_TLS_VERIFY=0 >> /etc/sysconfig/docker'
  sh -c ' echo other_args=\"--storage-opt dm.basesize=30G --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2376\" >> /etc/sysconfig/docker'
  service docker restart
}

format_disks() {
  START_LABEL=platform_disk_start_label
  mkdir /hadoopfs
  for (( i=1; i<=15; i++ )); do
    LABEL=$(printf "\x$((START_LABEL+i))")
    if [ -e /dev/platform_disk_prefix"$LABEL" ]; then
      mkfs -F -t ext4 /dev/platform_disk_prefix${LABEL}
      mkdir /hadoopfs/fs${i}
      echo /dev/platform_disk_prefix${LABEL} /hadoopfs/fs${i} ext4  defaults 0 2 >> /etc/fstab
      mount /hadoopfs/fs${i}
      DOCKER_VOLUME_PARAMS="${DOCKER_VOLUME_PARAMS} -v /hadoopfs/fs${i}:/hadoopfs/fs${i}"
    fi
  done
}

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  elif [ ! -f "/var/cb-init-executed" ]; then
    format_disks
    fix_hostname
    configure_docker
    touch /var/cb-init-executed
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"