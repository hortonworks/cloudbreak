#!/bin/bash

# For usage, please type 
#   ./increase-root-disk-1-azure.sh
#
# By default, dry run is turned off. To turn it on, please set it to true
DRY_RUN=false

SCRIPT_NAME_BASE=increase-root-disk-1-azure
SCRIPT_NAME=$SCRIPT_NAME_BASE.sh
LOG_FILE=$SCRIPT_NAME_BASE.log
DESCRIBE_INSTANCE_FILE="$SCRIPT_NAME_BASE-describe-instances-result.json"
DESCRIBE_VOLUME_FILE="$SCRIPT_NAME_BASE-describe-volumes-result.json"


TAB="    "
LI="- "

show_usage() {
    message
    message USAGE
    message =====
    message
    message ./$SCRIPT_NAME \<TARGET_ROOT_VOLUME_SIZE\> \<RESOURCE_GROUP_NAME\> \<VM_NAME_LIST\>
    message
    message "${TAB}${LI}TARGET_ROOT_VOLUME_SIZE: The size of the new root volumes, in GB (do NOT specify the unit, just the numbers, please). It is recommended to have at least 100GB space available on the root volume"
    message "${TAB}${LI}RESOURCE_GROUP_NAME: name of the resource group where your VMs are"
    message "${TAB}${LI}VM_NAME_LIST: name (not the full resource ID) of the VMs"
    message
    message
    message GET ALL INSTANCES FROM DL / DH
    message ==============================
    message 
    message "${LI}Datalake:"
    message "${TAB}cdp datalake describe-datalake \ "
    message "${TAB}${TAB}--datalake-name <YOUR_DATALAKE_NAME> > describe-datalake-result.json"
    message "${TAB}cat describe-datalake-result.json | jq -r '.datalake.instanceGroups[] | .instances[] | .id' | tr '\n' ' '"
    message
    message "${LI}Data Hub"
    message "${TAB}cdp datahub describe-cluster  \ "
    message "${TAB}${TAB}--cluster-name <YOUR_CLUSTER_NAME> > describe-dh-result.json"
    message "${TAB}cat describe-dh-result.json | jq -r '.cluster.instanceGroups[] | .instances[] | .id' | tr '\n' ' '"
    message
    message
    message PREREQUISITES
    message =============
    message
    message "${TAB}- az-cli"
    message "${TAB}- jq"
    message
    message Please save output of STDOUT and log file change-image-in-lt.log in case there is an error.
}

error_and_exit() {
    message
    message "*** ERROR: "$*", exiting"
    log ERROR: "$*"
    message
    message Please save and zip following outputs and files \(if they exist\):
    message "${TAB}${LI} STDOUT"
    message "${TAB}${LI} the log file $LOG_FILE"
    message "${TAB}${LI} output file $DESCRIBE_INSTANCE_FILE"
    message "${TAB}${LI} output file $DESCRIBE_VOLUME_FILE"
    message and contact Cloudera support.
    message Exiting.
    exit
}

error_and_exit_no_log() {
    message
    message "*** ERROR: $*"
    log ERROR: "$*"
    message
    message Exiting.
    exit
}

error_and_exit_no_collect_info() {
    message
    message !!! ERROR: "$*"
    log ERROR: "$*"
    message
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

check_prerequisites() {
    message 1. Checking prerequisites
    message
    az_version=$(az version | tr '\n' ' ')
    if [ -z "$az_version" ]; then
        error_and_exit_no_collect_info It seems you do not have az-cli installed. Please install it and rerun the script.
    fi
    message "${TAB}az-cli version: '$az_version'"

    jq_version=$(jq --version)
    if [ -z "$jq_version" ]; then
        error_and_exit_no_collect_info It seems you do not have jq installed. Please install it and rerun the script.
    fi
    message "${TAB}jq version: '$jq_version'"
}

describe_instances() {
    local rg=$1
    local instance_id_list="$2"
    log instance id list in describe isntances: $instance_id_list

    az vm list \
        --resource-group $rg \
        --query "[?contains(\`[$instance_id_list]\`, name)].{Name:name, PowerState:powerState, OsDiskId:storageProfile.osDisk.managedDisk.id, OsDiskName:storageProfile.osDisk.name}" \
        --show-details > $DESCRIBE_INSTANCE_FILE
    if [ $? -ne 0 ]; then 
        error_and_exit could not retrieve instance info on instance $instance_id_list
    fi
    log azure instance data: $(cat $DESCRIBE_INSTANCE_FILE)
}

validate_all_instances_are_present() {
    expected_vms="$1"

    present_vms=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.[] | .Name')
    missing_vms=""
    for vm in $expected_vms; do
        echo $present_vms | grep $vm > /dev/null
        if [ $? -ne 0 ]; then
            missing_vms="$missing_vms $vm"
        else
            echo vm $vm is present
        fi
    done

    if [ ! -z "$missing_vms" ]; then
        error_and_exit "Could not retrieve info about one or more VMs from azure. Please make sure that RESOURCE_GROUP_NAME and VM_NAME_LIST are correct and try again. Missing VMs: $missing_vms"
    fi
}

validate_instances_are_stopped() {
    # $(cat $DESCRIBE_INSTANCE_FILE | jq -r '.[] | .Name, .PowerState' | sed "N;s/\n/${TAB}=>${TAB}/")
    message
    instance_id_to_instance_state=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.[] | "        \(.Name) => \(.PowerState)"')
    message "${TAB}The state of instances:"
    message "$instance_id_to_instance_state"
    not_stopped_instances=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.[] | select(.PowerState != "VM deallocated") | .Name' | tr '\n' ' ')
    if [ ! -z "$not_stopped_instances" ]; then 
        error_and_exit_no_log "Some instances are not in stopped state: $not_stopped_instances. Please stop them and rerun the script."
    fi
    message "${TAB}All instances are stopped, can proceed."
    message
}

describe_volumes() {
    local rg=$1
    local disk_name_list="$2"

    az disk list \
        --resource-group $rg \
        --query "[?contains(\`[$disk_name_list]\`, name)].{Name:name,Size:diskSizeGb,Tier:sku.name}" \
        --output json > $DESCRIBE_VOLUME_FILE

    if [ $? -ne 0 ]; then 
        error_and_exit could not retrieve disk info on one or more disks $disk_id_list
    fi

}

show_root_volume_to_size_map() {
    local volume_id_to_size_map=$(cat $DESCRIBE_VOLUME_FILE | jq -r '.[] | "        \(.Name) => \(.Size)"')
    message
    message "${TAB}Root volume ids and their current size"
    message "$volume_id_to_size_map"
    message
}

set_root_volumes() {
    local rg=$1
    local new_root_volume_size=$2
    local new_root_volume_size_text="$3"
    shift 3

    while [ ! -z "$1" ]; do
        disk_name=$1
        message
        message "${TAB}${TAB}now setting disk size to $new_root_volume_size_text on disk $disk_name"
        if [ "$DRY_RUN" = true ]; then
            echo "*** Dry run: disks not modified"
        else
            disk_update_output=$(az disk update \
                --resource-group $rg \
                --name $disk_name \
                --size-gb $new_root_volume_size )
            if [ ! $? -eq 0 ]; then
                error_and_exit An error occurred when trying to set the new root disk size
            fi
        fi
        log result of modification: $disk_update_output
        message "${TAB}${TAB}root disk $disk_name was set to size $new_root_volume_size_text"
        shift
    done
}

main() {
    message
    message Increasing root volume, V1.0 - resizing OS disk
    message ===============================================
    message
    message OVERVIEW
    message ========
    message
    message Increasing the root volume on azure is composed of two steps:
    message "${TAB}1. increase the root disk itself"
    message "${TAB}2. increase the partition and file system on the running VM"
    message
    message This script is part 1.
    message
    message Takes a target root volume size, a resource group name and a list of VM names and will icrease the root volume of the specified VMs to the target size.
    message Will increase the root volume only if the required TARGET_ROOT_VOLUME_SIZE is bigger than the actual size of the root disk on a VM.
    message 
    message Before proceeding please do stop the cluster where you want to resize root volumes:
    message "${TAB}${LI}DH: please stop the DH itself"
    message "${TAB}${LI}DL: please stop all the attahed DHs and then the DL itself"

    if [ "$DRY_RUN" = true ]; then
        message
        message "========================================================"
        message "*** Running in DRY_RUN mode, nothing will be changed ***"
        message "========================================================"
        message
    fi

    message

    log Invocation: $0 $*

    # parse parameters
    new_root_volume_size=$1
    new_root_volume_size_text="$new_root_volume_size GB"
    if [ -z "$new_root_volume_size" ]; then
        show_usage
        error_and_exit_no_log TARGET_ROOT_VOLUME_SIZE is missing
    fi

    resource_group=$2    
    if [ -z "$resource_group" ]; then
        show_usage
        error_and_exit_no_log RESOURCE_GROUP_NAME is missing
    fi

    instance_id_list=""
    shift 2
    while [ ! -z "$1" ]; do
        instance_id_list="$instance_id_list $1"
        shift
    done

    if [ -z "$instance_id_list" ]; then
        show_usage
        error_and_exit_no_log VM_NAME_LIST is missing
    fi

    message
    message Will set root volume to $new_root_volume_size_text on instances $instance_id_list
    message

    check_prerequisites

    # describe all instances
    message
    message 2. Checking instances
    log get info of instance
    describe_instances $resource_group "$instance_id_list"
    validate_all_instances_are_present "$instance_id_list"

    # assert all VMs are deallocated
    validate_instances_are_stopped

    # find root volumes of instances
    message 3. Checking and changing root volume size in the instances
    root_volume_names=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.[] | .OsDiskName' | tr '\n' " ")
    log found root volume names: "$root_volume_names"

    # describe volumes, and get those where the size is smaller than the desired size

    describe_volumes $resource_group "$root_volume_names"
    show_root_volume_to_size_map
    root_volumes_to_set=$(cat $DESCRIBE_VOLUME_FILE | jq -r --arg VOLUME_SIZE "$new_root_volume_size" '.[] | select(.Size < ($VOLUME_SIZE|tonumber)) | .Name' | tr '\n' ' ')

    if [ -z "$root_volumes_to_set" ]; then
        message
        message RESULT: No root volumes found where the size is smaller than $new_root_volume_size_text. Exiting
        exit
    fi

    message "${TAB}Root volumes where the size is smaller than $new_root_volume_size_text: $root_volumes_to_set"
    message "${TAB}Setting them to new_root_volume_size_text"

    # modify volumes where the root disk size is maller than desired
    set_root_volumes $resource_group $new_root_volume_size "$new_root_volume_size_text" $root_volumes_to_set

    message
    message RESULT: Finished successfully. The required volumes were increased to $new_root_volume_size_text.
    message 
    message Now please use increase-root-disk-2-azure.sh to increase partition and filesystem size to $new_root_volume_size_text.
}

main $*
