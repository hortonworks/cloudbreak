#!/usr/bin/env bash

SEMAPHORE_FILE=/var/cb-mount-executed
FS_TYPE=ext4
EPHEMERAL_FS_TYPE=ext4
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
}

script_executed_successfully() {
    local script_name=$1
    echo "$(date +%Y-%m-%d:%H:%M:%S) - $script_name executed" >> $SEMAPHORE_FILE
}

is_cloud_platform_supported() {
    local log_file=$1
    case $CLOUD_PLATFORM in
        AWS|AWS_NATIVE|AWS_NATIVE_GOV|AZURE|GCP) ;; # "Cloud platform is supported"
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
    local device_name=$1 # input is expected without '/dev/'
    local log_file=$2
    nvme list | grep $device_name | sed 's/ \+/ /g' | tr '[:upper:]' '[:lower:]' | grep "amazon elastic block store" >> $log_file 2>&1
    [[ $? -eq 0 ]] && return 1 || return 0
}

lsblk_command() {
    if [[ -z $(lsblk -I 8,259 -dn) ]]; then
        lsblk -dn
    else
        lsblk -I 8,259 -dn
    fi
}

are_all_attached_volume_names_present() {
  local log_file=$1
    log $log_file searching for devices: $ATTACHED_VOLUME_NAME_LIST
    local existing_device_count=0
    for device in $ATTACHED_VOLUME_NAME_LIST; do
        if [[ -e $device ]]; then
            ((existing_device_count++))
        else
           log $LOG_FILE device $device was not found
        fi
    done
    local device_name_array=($ATTACHED_VOLUME_NAME_LIST)
    return $([[ $existing_device_count -eq ${#device_name_array[@]} ]])
}

strip_serial_id() {
    local serial_id=$1
    serial_id_stripped=$(echo $serial_id | sed 's/\-//g')
    echo $serial_id_stripped
}

get_nvme_device_names() {
    local log_file=$1
    local return_value=0

    declare -A serial_id_to_device_path_map
    declare -A serial_id_stripped_to_device_path_map

    for nvme_device in $(nvme list -o json | jq -r '.Devices[] | "\(.SerialNumber)=\(.DevicePath)"'); do
        IFS='=' read -r -a split_array <<< "$nvme_device"
        serial_id_to_device_path_map[${split_array[0]}]=${split_array[1]}
        serial_id_stripped=$(strip_serial_id ${split_array[0]})
        serial_id_stripped_to_device_path_map[$serial_id_stripped]=${split_array[1]}
    done

    nvme_device_list=()
    for volume_serial_id in $ATTACHED_VOLUME_SERIAL_LIST; do
        found_device=${serial_id_to_device_path_map[$volume_serial_id]}
        log $log_file searched $volume_serial_id, found \"$found_device\"
        if [ -z "$found_device" ]; then
            serial_id_stripped=$(strip_serial_id $volume_serial_id)
            found_device=${serial_id_stripped_to_device_path_map[$serial_id_stripped]}
            log $log_file searched $serial_id_stripped, found \"$found_device\"
        fi
        if [ -z "$found_device" ]; then
            log $log_file not found device for volume id: $volume_serial_id
            return_value=1
        else
            echo $found_device
        fi
    done

    return $((return_value))
}

get_device_names() {
    local log_file=$1
    if $(are_all_attached_volume_names_present $log_file) ; then
        echo $ATTACHED_VOLUME_NAME_LIST
        return 0
    else
        nvme_device_list=$(get_nvme_device_names $log_file)
        local return_value=$?
        echo $nvme_device_list
        return $((return_value))
    fi
}

get_uuid_list() {
  local log_file=$1
  local device_name_list=$2
  local uuid_list=""
  for device in $device_name_list; do
      uuid=$(get_disk_uuid $device)
      if [ -z "$(blkid $device)" ]; then
          log log_file "uuid still does not exists for device $device => error"
      else
        if [[ $uuid_list == "" ]]; then
            uuid_list=$uuid
        else
            uuid_list="$uuid_list $uuid"
        fi
      fi
  done
  echo $uuid_list
}

get_mounted_instance_storage_count() {
  if [[ $(nvme list) ]]; then
    df -h | grep -E "$(nvme list | grep "Instance Storage" | awk '{printf "%s%s", NR==1? "" : "|", $1} END{print ""}')" | grep -c "/hadoopfs/fs"
  else
    OSDEVICE=$(lsblk -o NAME -n | grep -v '[[:digit:]]' | sed "s/^sd/xvd/g")
    # THIS IS BASE DEVICE MAPPING FOR AWS - THE URL WILL NOT CHANGE; THIS IS FOR CHECKING IF AN ATTACHED DEVICE IS AN EPHEMERAL DEVICE
    BDMURL="http://169.254.169.254/latest/meta-data/block-device-mapping/"
    IMDS_TOKEN=`curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"`
    df -h | grep -E "$(for bd in $(curl -s -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" ${BDMURL});
                        do
                          MAPDEVICE=$(curl -s -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" ${BDMURL}/${bd}/ | sed "s/^sd/xvd/g");
                          if grep -wq ${MAPDEVICE} <<< "${OSDEVICE}"; then
                            echo "${MAPDEVICE}"
                          fi
                        done)" | grep -c "/hadoopfs/fs"
  fi
}

get_all_mount_paths_from_fstab() {
  grep /hadoopfs/ /etc/fstab | awk '{print $2}'
}