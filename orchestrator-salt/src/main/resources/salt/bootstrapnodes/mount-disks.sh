#!/usr/bin/env bash
source format-and-mount-common.sh

LOG_FILE=/var/log/mount-disks.log
VERSION="V1.0"

# INPUT
#   ATTACHED_VOLUME_UUID_LIST - contains a list of uuids of volumes attached to the instance, format: space separated list
#   PREVIOUS_FSTAB - contains the fstab if any from a previous instance, format: the file contents as is
#
# OUTPUT
#   happy path:
#       exit code: 0
#
#   error:
#       exit code: not 0
#       stderr: a one line info. Details are in the log

mount_remaining() {
      local hadoop_fs_dir_counter=$1
      local return_value=0
      if [[ "$CLOUD_PLATFORM" == "AZURE" ]]; then
        not_mounted_volume_names=$(lsblk_command --noheadings --raw -o NAME,MOUNTPOINT | awk '$1~/[[:digit:]]/ && $2 == ""')
      else
        not_mounted_volume_names=$(lsblk_command | grep -v / | grep ^[a-z] | cut -f1 -d' ')
      fi
      log $LOG_FILE remaining not mounted volumes: $not_mounted_volume_names
      root_disk=$(get_root_disk)
      for volume in $not_mounted_volume_names; do
          uuid=$(get_disk_uuid "/dev/$volume")
          if [[ $root_disk != "/dev/$volume" && "$uuid" != ""  ]] && not_elastic_block_store "/dev/$volume" $LOG_FILE; then
                mount_one "UUID=$uuid /hadoopfs/fs${hadoop_fs_dir_counter} $FS_TYPE defaults,noatime,nofail 0 2"
                return_value=$(($? || return_value ))
                log $LOG_FILE result of all mounting: $return_value
                ((hadoop_fs_dir_counter++))
           elif [[ "$root_disk" == "/dev/$volume" ]]; then
                log $LOG_FILE volume $volume is the root volume, skipping it
           elif ! not_elastic_block_store "/dev/$volume" $LOG_FILE ; then
                log $LOG_FILE volume $volume is a free-rider volume, skipping it
           else
                log $LOG_FILE volume $volume has still no uuid, skipping it
                return_value=1
           fi
      done

    return $((return_value))
}

mount_all_from_fstab() {
      local return_value=0
      local hadoop_fs_dir_counter=1
      log $LOG_FILE mounting via fstab, value: "$PREVIOUS_FSTAB"
      for uuid in $ATTACHED_VOLUME_UUID_LIST; do
          local fstab_line=$(echo "$PREVIOUS_FSTAB" | grep $uuid)
          mount_one "$fstab_line"
          mount_result=$?
          [[ $mount_result -eq 0 ]] && ((hadoop_fs_dir_counter++))
          return_value=$(($? || return_value ))
      done

      mount_remaining $hadoop_fs_dir_counter
      return_value=$(($? || return_value ))

      return $((return_value))
}

mount_all_sequential() {
      local return_value=0
      log $LOG_FILE mounting for first time, no previous fstab information present
      local hadoop_fs_dir_counter=1
      for uuid in $ATTACHED_VOLUME_UUID_LIST; do
          mount_one "UUID=$uuid /hadoopfs/fs${hadoop_fs_dir_counter} $FS_TYPE defaults,noatime,nofail 0 2"
          return_value=$(($? || return_value ))
          log $LOG_FILE result of all mounting: $return_value
          ((hadoop_fs_dir_counter++))
      done

      mount_remaining $hadoop_fs_dir_counter
      return_value=$(($? || return_value ))

      return $((return_value))
}

mount_one() {
      local return_value=0
      local success=0
      local fstab_line=$1
      local path=$(echo $fstab_line | cut -d' ' -f2)

      log $LOG_FILE mounting to path $path, line in fstab: $fstab_line
      mkdir $path >> $LOG_FILE 2>&1
      echo $fstab_line >> /etc/fstab
      log $LOG_FILE result of editing fstab: $?
      mount $path >> $LOG_FILE 2>&1
      if [ ! $? -eq 0 ]; then
        log $LOG_FILE error mounting device on $path
        return_value=1
      fi
      log $LOG_FILE result of mounting $path: $?
      chmod 1777 $path >> $LOG_FILE 2>&1
      return $((return_value))
}

mount_common() {
    local return_value
    mkdir /hadoopfs
    if [[ -z $PREVIOUS_FSTAB  ]]; then
        mount_all_sequential
        return_value=$?
    else
        mount_all_from_fstab
        return_value=$?
    fi
    return $((return_value))
}

create_directories() {
    cd /hadoopfs/fs1 && mkdir logs >> $LOG_FILE 2>&1

    fs1_logs_dir=/hadoopfs/fs1/logs
    [[ -d $fs1_logs_dir ]] && return 0

    log $LOG_FILE there was an error creating log directories in /hadoopfs/fs1
    return 1
}

grow_mount_partition() {
    # TODO: remove this line if cloud-utils-growpart is already on the image
    yum -y install cloud-utils-growpart
    if growpart -N $(get_root_disk) $(get_root_disk_partition_number) ; then
        log $LOG_FILE Before growing: $(df -h)
        growpart $(get_root_disk) $(get_root_disk_partition_number)
        if [ ! $? -eq 0 ]; then
          log $LOG_FILE error growing partition
          return 1
        fi
        if [ "$(get_root_disk_type)" == "xfs" ]; then
            xfs_growfs /
            return_value=$?
        elif [[ "$(get_root_disk_type)" =~ "ext" ]]; then
            resize2fs  $(get_root_disk)$(get_root_disk_partition_number)
            return_value=$?
        fi
        log $LOG_FILE After growing: $(df -h)
        return $((return_value))
    else
        log $LOG_FILE Growpart indicates that there is no need for growing root partition
    fi
}

save_env_vars_to_log_file() {
    log $LOG_FILE environment variables:
    log $LOG_FILE ATTACHED_VOLUME_UUID_LIST=$ATTACHED_VOLUME_UUID_LIST
    log $LOG_FILE CLOUD_PLATFORM=$CLOUD_PLATFORM
    log $LOG_FILE PREVIOUS_FSTAB=$PREVIOUS_FSTAB
}

main() {
    log $LOG_FILE "started, version: $VERSION"
    save_env_vars_to_log_file
    local script_name="mount-disk"
    can_start $script_name $LOG_FILE

    mount_common
    return_code=$?
    [[ ! $return_code -eq 0 ]] && exit_with_code $LOG_FILE $return_code "Not all devices were mounted"

    if [[ "$CLOUD_PLATFORM" == "AZURE" ]]; then
       grow_mount_partition
       return_code=$?
       [[ ! $return_code -eq 0 ]] && exit_with_code $LOG_FILE $return_code "Error growing root partition"
    fi

    create_directories
    return_code=$?
    [[ ! $return_code -eq 0 ]] && exit_with_code $LOG_FILE $return_code "Error creating directories on /hadoopfs/fs1"

    exit_with_code $LOG_FILE 0 "Script $script_name ended"
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
