#!/usr/bin/env bash

SEMAPHORE_FILE=/var/cb-mount-executed
FS_TYPE=ext4
EXIT_CODE_ERROR=1
EXIT_CODE_OK=0

log() {
    local log_file=$1
    shift
    echo $(date) $@ >> "$log_file"
}

log_command() {
    local log_file=$1
    local command=$2
    shift 2
    log $log_file executing command $command
    $($command) >> "$log_file" 2>&1
}

output() {
    echo $@
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

semaphore_file_exists() {
    local log_file=$1
    if [ ! -f "$SEMAPHORE_FILE" ]; then
        log $log_file "semaphore file $SEMAPHORE_FILE missing, cannot proceed. Exiting"
        exit 1
    fi
}

was_script_executed() {
    local script_name=$1
    local log_file=$2
    script_executed=$(grep $script_name "$SEMAPHORE_FILE")
    log $log_file script executed = "$script_executed"
    if [ ! -z "$script_executed" ]; then
        log $log_file "script $script_name was already executed, line from semaphore file: $script_executed. Exiting"
        exit
    fi
    echo "$(date +%Y-%m-%d:%H:%M:%S) - $script_name executed" >> $SEMAPHORE_FILE
}

is_cloud_platform_supported() {
    local log_file=$1
    case $CLOUD_PLATFORM in
        AWS|AZURE|GCP) ;; # "Cloud platform is supported"
        *)         log $log_file "Cloud platform is NOT supported. Cloud platform currently: $CLOUD_PLATFORM. Exiting"
                   exit;;
    esac
}

can_start() {
    local script_name=$1
    local log_file=$2
    log $log_file starting script $script_name
    is_cloud_platform_supported $log_file
    semaphore_file_exists $log_file
    was_script_executed $script_name $log_file
}

get_disk_uuid() {
    local device=$1
    uuid=$(blkid -o export $device | grep -i UUID | cut -d '=' -f 2)
    local retval=$?
    echo $uuid
    return $((retval))
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

not_elastic_block_store() {
    local device_name=$1 # input is evpected without '/dev/'
    local log_file=$2
    nvme list | grep $device_name | sed 's/ \+/ /g' | tr '[:upper:]' '[:lower:]' | grep "amazon elastic block store" >> $log_file 2>&1
    [[ $? -eq 0 ]] && return 1 || return 0
}

lsblk_command() {
    if [[ -z $(lsblk -I 8 -dn) ]]; then
        lsblk -dn
    else
        lsblk -I 8 -dn
    fi
}
