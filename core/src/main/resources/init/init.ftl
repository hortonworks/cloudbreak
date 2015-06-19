#!/bin/bash
set -x

START_LABEL=${platformDiskStartLabel}
PLATFORM_DISK_PREFIX=${platformDiskPrefix}

<#if gateway>
setup_tmp_ssh() {
  echo "${tmpSshKey}" >> /home/${sshUser}/.ssh/authorized_keys
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

print_ssh_fingerprint() {
    echo "cb: -----BEGIN SSH HOST KEY FINGERPRINTS-----"
    echo "cb: $(ssh-keygen -lf /etc/ssh/ssh_host_rsa_key.pub)"
    echo "cb: -----END SSH HOST KEY FINGERPRINTS-----"
}

format_disks() {
  mkdir /hadoopfs
  for (( i=1; i<=24; i++ )); do
    LABEL=$(printf "\x$(printf %x $((START_LABEL+i)))")
    DEVICE=/dev/${PLATFORM_DISK_PREFIX}${LABEL}
    if [ -e $DEVICE ]; then
      MOUNTPOINT=$(grep $DEVICE /etc/fstab | tr -s ' \t' ' ' | cut -d' ' -f 2)
      if [ -n "$MOUNTPOINT" ]; then
        umount "$MOUNTPOINT"
        sed -i "\|^$DEVICE|d" /etc/fstab
      fi
      mkfs -E lazy_itable_init=1 -O uninit_bg -F -t ext4 $DEVICE
      mkdir /hadoopfs/fs${i}
      echo $DEVICE /hadoopfs/fs${i} ext4  defaults 0 2 >> /etc/fstab
      mount /hadoopfs/fs${i}
    fi
  done
  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch logs/consul-watch-db logs/consul-watch-main
}
</#noparse>

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  elif [ ! -f "/var/cb-init-executed" ]; then
    <#if gateway>
    setup_tmp_ssh
    </#if>
    print_ssh_fingerprint
    format_disks
    fix_hostname
    configure_docker
    touch /var/cb-init-executed
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-init-executed
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"