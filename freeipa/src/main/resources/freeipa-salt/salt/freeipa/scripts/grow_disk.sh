#!/usr/bin/env bash

LOG_FILE=/var/log/grow-disks.log
VERSION="V1.0"

log() {
    local log_file=$1
    shift
    echo $(date) $@ >> "$log_file"
}

exit_with_code() {
    local log_file=$1
    local error_code=$2
    shift 2
    local error_message="$@, exiting with code: $error_code."
    log $log_file $error_message
    [[ ! $error_code -eq 0 ]] && echo $error_message >&2
    exit $((error_code))
}

get_root_disk() {
    root_partition=$(lsblk | grep /$ | cut -f1 -d' ' )
    if [[ $root_partition =~ "nvme" ]]; then
        echo "/dev/$(lsblk | grep /$ | cut -f1 -d' ' | sed 's/p\w//g' | cut -c 3-)"
    else
        echo "/dev/$(lsblk | grep /$ | cut -f1 -d' ' | sed 's/[0-9]//g' | cut -c 3-)"
    fi
}

get_root_disk_partition_number() {
    echo $(lsblk --output MAJ:MIN,MOUNTPOINT --raw | grep /$ | cut -f1 -d' ' | cut -f2 -d':')
}

get_root_disk_type() {
    echo $(lsblk --output FSTYPE,MOUNTPOINT --raw | grep /$ | cut -f1 -d' ')
}

grow_mount_partition() {
    set -x
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
    set +x
}

main() {
    log $LOG_FILE "started, version: $VERSION"

    if [[ "$CLOUD_PLATFORM" == "AZURE" ]]; then
       grow_mount_partition
       return_code=$?
       [[ ! $return_code -eq 0 ]] && exit_with_code $LOG_FILE $return_code "Error growing root partition"
    fi

    exit_with_code $LOG_FILE 0 "grow-disk script has run successfully"
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
