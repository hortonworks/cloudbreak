#!/usr/bin/env bash

SEMAPHORE_FILE=/var/cb-mount-executed

format_disks() {
  lazy_format_disks
  cd /hadoopfs/fs1 && mkdir logs logs/ambari-server logs/ambari-agent logs/consul-watch logs/kerberos
}

lazy_format_disks() {
  FS_TYPE=ext4
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
      if [ -z "$(blkid $DEVICE | grep $FS_TYPE)" ]; then
          echo "formatting: $DEVICE"
          mkfs -E lazy_itable_init=1 -O uninit_bg -F -t $FS_TYPE $DEVICE
      fi
      # TODO make mount idempotent
      mkdir /hadoopfs/fs${i}
      echo UUID=$(blkid -o value $DEVICE | head -1) /hadoopfs/fs${i} $FS_TYPE  defaults,noatime,nofail 0 2 >> /etc/fstab
      mount /hadoopfs/fs${i}
      chmod 777 /hadoopfs/fs${i}
    fi
  done
}

main() {
    local script_name="mount-disk"
    if [ ! -f "$SEMAPHORE_FILE" ]; then
        echo "semaphore file $SEMAPHORE_FILE missing, cannot proceed. Exiting"
        exit
    fi
    script_executed=$(grep $script_name "$SEMAPHORE_FILE")
    if [ -z "$script_executed" ]; then
        [[ $CLOUD_PLATFORM == "AWS" ]] && format_disks
        echo "$(date +%Y-%m-%d:%H:%M:%S) - $script_name executed" >> $SEMAPHORE_FILE
    fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"