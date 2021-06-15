#!/bin/bash

# For usage, please type 
#   ./increase-root-volume-azure.sh
#

# By default, dry run is turned on - the script has no parameters. To turn it on, please set it to true
DRY_RUN=true

SCRIPT_NAME_BASE=increase-root-disk-2-azure
SCRIPT_NAME=$SCRIPT_NAME_BASE.sh
LOG_FILE=$SCRIPT_NAME_BASE.log
OUT_FILE=$SCRIPT_NAME_BASE.out

SIZE_EPSILON=2
SIZE_EPSILON_BYTES=2147483648
KBYTES=1024
TAB="    "

error_and_exit() {
    message
    message !!! ERROR: "$*", exiting
    log ERROR: "$*"
    message
    message Please save and zip following outputs and files \(if they exist\):
    message "${TAB}- STDOUT"
    message "${TAB}- the log file $LOG_FILE"
    message "${TAB}- output file $DESCRIBE_INSTANCE_FILE"
    message "${TAB}- output file $DESCRIBE_VOLUME_FILE"
    message and contact Cloudera support.
    message Exiting.
    exit
}

log() {
    echo $(date) $* >> $LOG_FILE
}

message() {
    echo "$*"
    log "$*"
}

main() {
    message
    message Increasing root volume, V1.0 - resizing partition and filesystem of OS Disk
    message =======================================
    message
    message Increasing the root volume on azure is composed of two steps:
    message "${TAB}1. increase the root disk itself"
    message "${TAB}2. increase the partition on the running instance"
    message
    message This script is part 2.
    message It will only increase partition or filesystem size if the current size is smaller than allowed by the disk.
    message
    message How to use it:
    message "${TAB}1. log in to the master node and make yourself root"
    message "${TAB}2. activate the salt environment"
    message "${TAB}3. copy this script to the master node"
    message "${TAB}4. distribute this script to all nodes: salt-cp '*' $SCRIPT_NAME /home/cloudbreak"
    message "${TAB}5. make the script executable on all nodes: salt '*' cmd.run 'chmod 744 /home/cloudbreak/$SCRIPT_NAME'"
    message "${TAB}6. run the script on every node and save output: salt '*' cmd.run './home/cloudbreak/$SCRIPT_NAME' > $OUT_FILE"
    message "${TAB}7. check the output that it succeeded on every node (you can compare it with the sample output provided in the README)"
    message ""
    message started: $(date)

    message 
    if [ "$DRY_RUN" = true ]; then
        message
        message "========================================================"
        message "*** Running in DRY_RUN mode, nothing will be changed ***"
        message ""
        message "    To turn it off, please open the script and set"
        message "    variable DRY_RUN to false                     "
        message "========================================================"
        message
    fi

    partition_path=$(lsblk -lp -o NAME,SIZE,PKNAME,MOUNTPOINT | grep "/$" | tr -s ' ' | cut -f1 -d' ')
    partition_size_bytes=$(lsblk -lpb -o NAME,SIZE,PKNAME,MOUNTPOINT | grep "/$" | tr -s ' ' | cut -f2 -d' ')
    partition_size_gb=$(lsblk -lp -o NAME,SIZE,PKNAME,MOUNTPOINT | grep "/$" | tr -s ' ' | cut -f2 -d' ')
    disk_path=$(lsblk -lp -o NAME,SIZE,PKNAME,MOUNTPOINT | grep "/$" | tr -s ' ' | cut -f3 -d' ')
    disk_size_bytes=$(lsblk -lpb -o NAME,SIZE | grep -sw "$disk_path" | tr -s ' ' | cut -f2 -d' ')
    disk_size_gb=$(lsblk -lp -o NAME,SIZE | grep -sw "$disk_path" | tr -s ' ' | cut -f2 -d' ')
    message "root disk properties: "
    message "${TAB}* path: $disk_path "
    message "${TAB}* size in bytes: $disk_size_bytes ($disk_size_gb GB)"
    message
    partition_number=$(echo $partition_path | sed "s@$disk_path@@g")
    message "root partition properties: "
    message "${TAB}* path: $partition_path"
    message "${TAB}* number: $partition_number"
    message "${TAB}* size in bytes: $partition_size_bytes ($partition_size_gb GB)"
    message
    if (( disk_size_bytes - partition_size_bytes > SIZE_EPSILON_BYTES )); then
        message "The partition $partition_path size $partition_size_bytes ($partition_size_gb GB) is much smaller than the disk size $disk_size_bytes ($disk_size_gb GB), resize is needed"
        if [ "$DRY_RUN" = true ]; then
            message "*** Running in DRY_RUN mode, command (growpart $disk_path $partition_number) not executed"
        else
            message executing growpart 
            growpart $disk_path $partition_number
            if [ ! $? -eq 0 ]; then
                error_and_exit An error occurred when trying to grow partition
            fi
            partition_grown_size=$(lsblk -lp -o NAME,SIZE,PKNAME,MOUNTPOINT | grep "/$" | tr -s ' ' | cut -f2 -d' '| sed 's/[^0-9,]*//g')
            message partition 

        fi
    else
        message "The partition $partition_path size $partition_size_bytes ($partition_size_gb GB) is almost as big as the disk size $disk_size_bytes ($disk_size_gb GB), no resize is needed"
    fi

    message
    filesystem_size_kb=$(df -h --block-size=1K --output=source,size | tr -s ' ' | grep $partition_path | cut -f2 -d' ')
    filesystem_size_gb=$(df -h --block-size=1G --output=source,size | tr -s ' ' | grep $partition_path | cut -f2 -d' ')
    message "root file system size is ${filesystem_size_kb} KB ($filesystem_size_gb GB)"
    message
    if (( disk_size_bytes - filesystem_size_kb * KBYTES > SIZE_EPSILON_BYTES )); then
        message file system needs resizing
        if [ "$DRY_RUN" = true ]; then
            message "*** Running in DRY_RUN mode, command (xfs_growfs /) not executed"
        else
            message executing xfs_growfs
            xfs_growfs /
            if [ ! $? -eq 0 ]; then
                error_and_exit An error occurred when trying to resize filesystem
            fi
        fi
    else
        message The file system on $partition_path size $partition_size_bytes is almost as big as the disk size $disk_size_bytes, no resize is needed
    fi

    message Finished
}

main $*