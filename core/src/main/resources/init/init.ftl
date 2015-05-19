#!/bin/bash
set -x

START_LABEL=${platformDiskStartLabel}
PLATFORM_DISK_PREFIX=${platformDiskPrefix}

<#if gateway>
start_proxy() {
  docker run --name gateway -d --net=host --restart=always sequenceiq/cb-gateway-nginx:0.1
}
</#if>

<#noparse>
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
  mkdir /hadoopfs
  for (( i=1; i<=15; i++ )); do
    LABEL=$(printf "\x$((START_LABEL+i))")
    if [ -e /dev/${PLATFORM_DISK_PREFIX}"$LABEL" ]; then
      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 /dev/${PLATFORM_DISK_PREFIX}${LABEL}
      mkdir /hadoopfs/fs${i}
      echo /dev/${PLATFORM_DISK_PREFIX}${LABEL} /hadoopfs/fs${i} ext4  defaults 0 2 >> /etc/fstab
      mount /hadoopfs/fs${i}
    fi
  done
}
</#noparse>

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  elif [ ! -f "/var/cb-init-executed" ]; then
    format_disks
    fix_hostname
    configure_docker
    <#if gateway>
    MAX_RETRIES=60
    retries=0
    while ((retries++ < MAX_RETRIES)) && ! docker info &> /dev/null; do echo "Docker is not running yet."; sleep 5; done
    start_proxy
    </#if>
    touch /var/cb-init-executed
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"