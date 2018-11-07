#!/usr/bin/env bash

format_disks() {
  lazy_format_disks
  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch logs/kerberos
}

lazy_format_disks() {
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
      echo UUID=$(blkid -o value $DEVICE | head -1) /hadoopfs/fs${i} ext4  defaults,noatime,nofail 0 2 >> /etc/fstab
      mount /hadoopfs/fs${i}
      chmod 777 /hadoopfs/fs${i}
    fi
  done
}

main() {
    if [ ! -f "/var/cb-mount-executed" ]; then
        [[ $CLOUD_PLATFORM == "AWS" ]] && format_disks
    fi
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/cb-mount-executed
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"