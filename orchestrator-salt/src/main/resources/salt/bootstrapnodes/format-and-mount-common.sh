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
        exit
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
    if [[ $CLOUD_PLATFORM != "AWS" ]]; then
        log $log_file "Only cloud platform AWS is supported. Cloud platform currently: $CLOUD_PLATFORM. Exiting"
        exit
    fi
}

can_start() {
    local script_name=$1
    local log_file=$2
    log $log_file starting script $script_name
    is_cloud_platform_supported $log_file
    semaphore_file_exists $log_file
    was_script_executed $script_name $log_file
}