#!/usr/bin/env bash
source format-and-mount-common.sh

LOG_FILE=/var/log/find_device_and_format.log
VERSION="V1.0"

# INPUT - expected lists
#   ATTACHED_VOLUME_NAME_LIST - contains a list of volume device names attached to the instance, format: space separated list
#   ATTACHED_VOLUME_SERIAL_LIST - contains a list of volume serial ids, format: space separated list
#
# OUTPUT:
#   happy case:
#       exit code: 0
#       STDOUT:   list of uuids, format: space  separated list
#
#   error:
#       exit code: 1
#       STDERR: a one-line summary of the type of error, without details. Details are in the log


unmount_device_if_mounted() {
      local device_name_list=$1
      local return_value=0
      log $LOG_FILE checking if devices are mounted: $device_name_list
      for device in $device_name_list; do
          local mountpoint=$(grep $device /etc/fstab | tr -s ' \t' ' ' | cut -d' ' -f 2)
          if [ -n "$mountpoint" ]; then
            $(umount "$mountpoint" >> $LOG_FILE 2>&1)
            log $LOG_FILE "unmounted $device, exit code of umount: $?"
            $(sed -i "\|^$device|d" /etc/fstab > $LOG_FILE 2>&1)
            log $LOG_FILE "deleting device $device from fstab, exit code of sed: $?"
            device_in_mount=$(mount | grep $device)
            if [ -n "$device_in_mount" ]; then
                log $LOG_FILE mount: device $device is still mounted, line from mount: $device_in_mount
                return_value=1
             fi
          else
            log $LOG_FILE "device $device is not yet mounted"
          fi
      done
      return $((return_value))
}

format_disks_if_unformatted() {
  local device_name_list=$1
  log $LOG_FILE format disk arguments: $device_name_list
  local return_value=0
  for devicename in $device_name_list; do
      log $LOG_FILE device: $devicename
      if [ -z "$(blkid $devicename)" ]; then
          log $LOG_FILE "formatting: $devicename"
          $(mkfs -E lazy_itable_init=1 -O uninit_bg -F -t $FS_TYPE $devicename >> $LOG_FILE 2>&1)
          if [ ! $? -eq 0 ]; then
            log $LOG_FILE "formatting of device $devicename failed"
            return_value=1
          fi
          log $LOG_FILE $format_result
      fi
  done
  return $((return_value))
}

get_uuid_list() {
  local device_name_list=$1
  local uuid_list=""
  for device in $device_name_list; do
      uuid=$(blkid -o value $device | head -1)
      if [ -z "$(blkid $device)" ]; then
          log $LOG_FILE "uuid still does not exists for device $device => error"
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

are_all_attached_volume_names_present() {
    log $LOG_FILE searching for devices: $ATTACHED_VOLUME_NAME_LIST
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

get_nvme_device_names() {
    local return_value=0

    declare -A serial_id_to_device_path_map
    for nvme_device in $(nvme list -o json | jq -r '.Devices[] | "\(.SerialNumber)=\(.DevicePath)"'); do
        IFS='=' read -r -a split_array <<< "$nvme_device"
        serial_id_to_device_path_map[${split_array[0]}]=${split_array[1]}
    done

    nvme_device_list=()
    for volume_serial_id in $ATTACHED_VOLUME_SERIAL_LIST; do
        found_device=${serial_id_to_device_path_map[$volume_serial_id]}
        log $LOG_FILE searched $volume_serial_id, found $found_device
        if [ -z "$found_device" ]; then
            log $LOG_FILE not found device for volume id: $volume_serial_id
            return_value=1
        else
            echo $found_device
        fi
    done

    return $((return_value))
}

get_device_names() {
    if $(are_all_attached_volume_names_present) ; then
        echo $ATTACHED_VOLUME_NAME_LIST
        return 0
    else
        nvme_device_list=$(get_nvme_device_names)
        local return_value=$?
        echo $nvme_device_list
        return $((return_value))
    fi
}

save_env_vars_to_log_file() {
    log $LOG_FILE environment variables:
    log $LOG_FILE ATTACHED_VOLUME_NAME_LIST=$ATTACHED_VOLUME_NAME_LIST
    log $LOG_FILE ATTACHED_VOLUME_SERIAL_LIST=$ATTACHED_VOLUME_SERIAL_LIST
    log $LOG_FILE CLOUD_PLATFORM=$CLOUD_PLATFORM
}

main() {
    log $LOG_FILE "started, version: $VERSION"
    save_env_vars_to_log_file
    local script_name="find-device-and-format"
    can_start $script_name $LOG_FILE

    device_name_list=$(get_device_names)
    if [ ! $? -eq 0 ]; then
        exit_with_code $LOG_FILE $EXIT_CODE_ERROR "some volumes are missing"
    fi
    log $LOG_FILE found devices: $device_name_list

    format_disks_if_unformatted "$device_name_list"
    if [ ! $? -eq 0 ]; then
        exit_with_code $LOG_FILE $EXIT_CODE_ERROR "could not format all devices"
    fi

    unmount_device_if_mounted "$device_name_list"
    if [ ! $? -eq 0 ]; then
        exit_with_code $LOG_FILE $EXIT_CODE_ERROR "could not unmount devices that were mounted by device name"
    fi

    local uuid_list=$(get_uuid_list "$device_name_list")
    log $LOG_FILE exit code of get_uuid_list: $?, uuids: $uuid_list
    output $uuid_list
    exit_with_code $LOG_FILE $EXIT_CODE_OK "script ran ok"
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
