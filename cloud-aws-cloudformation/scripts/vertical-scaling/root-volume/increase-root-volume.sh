#!/bin/bash

# For usage, please type 
#   ./increase-root-volume.sh
#
# By default, dry run is turned off. To turn it on, please set it to true
DRY_RUN=false

SCRIPT_NAME_BASE=increase-root-volume
SCRIPT_NAME=$SCRIPT_NAME_BASE.sh
LOG_FILE=$SCRIPT_NAME_BASE.log
DESCRIBE_INSTANCE_FILE="$SCRIPT_NAME_BASE-describe-instances-result.json"
DESCRIBE_VOLUME_FILE="$SCRIPT_NAME_BASE-describe-volumes-result.json"
DESCRIBE_AUTOSCALING_GROUP_FILE="$SCRIPT_NAME_BASE-describe-autoscaling-groups-result.json"

TAB="    "


show_usage() {
    message
    message PREREQUISITES
    message =============
    message
    message "${TAB}- aws-cli"
    message "${TAB}- jq"
    message "${TAB}- all instances have to be in stopped state"
    message 
    message
    message USAGE
    message =====
    message
    message ./$SCRIPT_NAME \<TARGET_ROOT_VOLUME_SIZE\> \<INSTANCE_ID_LIST\>
    message
    message "${TAB}- TARGET_ROOT_VOLUME_SIZE: The size of the new root volumes, in GB \(do NOT specify the unit, just the numbers, please\). It is recommended to have at least 100GB space available on the root volume"
    message "${TAB}- INSTANCE_ID_LIST: instance ids, separated by space (please see next section how to get them)"
    message
    message "The script only modifies settings if they are found to be below the target root volume size."
    message
    message "Dry run: please modify the script and set the DRY_RUN variable to true. In this way nothing will be changed. Default: false"
    message
    message "Note: there is a per volume rate limit on modifying EBS volumes. Once you hit that, you need to wait 6 hours."
    message
    message
    message GET ALL INSTANCES FROM DL / DH
    message ==============================
    message 
    message "- Datalake:"
    message "${TAB}cdp datalake describe-datalake \ "
    message "${TAB}${TAB}--datalake-name <YOUR_DATALAKE_NAME> > describe-datalake-result.json"
    message "${TAB}cat describe-datalake-result.json | jq -r '.datalake.instanceGroups[] | .instances[] | .id' | tr '\n' ' '"
    message
    message "- Data Hub"
    message "${TAB}cdp datahub describe-cluster  \ "
    message "${TAB}${TAB}--cluster-name <YOUR_CLUSTER_NAME> > describe-dh-result.json"
    message "${TAB}cat describe-dh-result.json | jq -r '.cluster.instanceGroups[] | .instances[] | .id' | tr '\n' ' '"
    message
    message
    message Please save output of STDOUT / STDERR and all files generated in case of an error.
}

error_and_exit() {
    message
    message "*** ERROR: $@, exiting"
    log ERROR: "$@"
    message
    message Please save and zip following outputs and files \(if they exist\):
    message
    message "  - STDOUT "
    message "  - the log file $LOG_FILE"
    message "  - output file $DESCRIBE_INSTANCE_FILE"
    message "  - output file $DESCRIBE_VOLUME_FILE"
    message "  - output file $DESCRIBE_AUTOSCALING_GROUP_FILE"
    message
    message and contact Cloudera support.
    message Exiting.
    exit
}

error_and_exit_no_log() {
    message
    message "*** ERROR: $@"
    log ERROR: "$*"
    message
    message Exiting.
    exit
}

greetings() {
    message
    message Increasing root volume, V1.0
    message ============================
    message
    message This script takes a target root volume size and a list of instance ids and will icrease the root volumes to the target size.
    message 
    message Before proceeding please do stop the cluster where you want to resize root volumes:
    message "${TAB}- DH: please stop the DH itself"
    message "${TAB}- DL: please stop all the attached DHs and then the DL itself"
    message

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
    aws_version=$(aws --version)
    if [ -z "$aws_version" ]; then
        error_and_exit_no_log It seems you do not have aws-cli installed. Please install it and rerun the script.
    fi
    message "    aws-cli version: '$aws_version'"

    jq_version=$(jq --version)
    if [ -z "$jq_version" ]; then
        error_and_exit_no_log It seems you do not have jq installed. Please install it and rerun the script.
    fi
    message "    jq version: '$jq_version'"
}

describe_instances() {
    aws ec2 describe-instances --instance-ids $instance_id_list > $DESCRIBE_INSTANCE_FILE
    if [ $? -ne 0 ]; then 
        error_and_exit could not retrieve instance info on instance $instance_id
    fi
    log ec2 instance data: $(cat $DESCRIBE_INSTANCE_FILE)
}

validate_instances_are_stopped() {
    instance_id_to_instance_state=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.Reservations[] | .Instances[] | "        \(.InstanceId) => \(.State.Name)"')
    message "${TAB}The state of instances:"
    message "$instance_id_to_instance_state"
    not_stopped_instances=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.Reservations[] | .Instances[] | select(.State.Name != "stopped") | .InstanceId' | tr '\n' ' ')
    if [ ! -z "$not_stopped_instances" ]; then 
        error_and_exit_no_log "Some instances are not in stopped state: $not_stopped_instances. Please stop them and rerun the script."
    fi
    message "${TAB}All instances are stopped, can proceed."
    message
}

get_root_volume_of_instances() {
    local instance_id_to_root_volume_name=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.Reservations[] | .Instances[] | "\(.InstanceId) \(.RootDeviceName)"' | tr '\n' ' ')
    log instance id to root volume id list: "$instance_id_to_root_volume_name"
    local root_volume_ids=$(get_root_volume_ids "$DESCRIBE_INSTANCE_FILE" $instance_id_to_root_volume_name)
    echo "$root_volume_ids"
}

get_root_volume_ids() {
    log Entering get_root_volume_ids
    log get_root_volume_ids incoming parameters: $@
    local describe_instances_result_file=$1
    shift
    local root_volume_ids=""
    while [ ! -z "$1" ] && [ ! -z "$2" ]; do
        local instance_id=$1
        local root_volume_name=$2
        log looking up root volume id, instance id: $instance_id, root volume name: $root_volume_name
        root_volume_id=$(cat $describe_instances_result_file | \
        jq -r \
            --arg INSTANCE_ID "$instance_id" \
            --arg VOLUME_NAME "$root_volume_name" \
        '.Reservations[] | .Instances[] | select(.InstanceId==$INSTANCE_ID)| .BlockDeviceMappings[] | select(.DeviceName==$VOLUME_NAME) | .Ebs.VolumeId')
        log found instance id: $instance_id, root volume name: $root_volume_name, root volume id: $root_volume_id
        root_volume_ids="$root_volume_ids $root_volume_id"
        shift 2
    done
    log exiting get_root_volume_ids
    echo "$root_volume_ids"
}

show_root_volume_to_size_map() {
    local volume_id_to_size_map=$(cat $DESCRIBE_VOLUME_FILE | jq -r '.Volumes[] | "        \(.VolumeId) => \(.Size) GB"')
    message
    message "${TAB}Root volume ids and their current size"
    message "$volume_id_to_size_map"
    message

}

set_root_volumes() {
    local new_root_volume_size=$1
    local new_root_volume_size_text="$2"
    shift 2

    while [ ! -z "$1" ]; do
        vol=$1
        message "${TAB}${TAB}now setting on $vol volume size to $new_root_volume_size_text"
        if [ "$DRY_RUN" = true ]; then
            echo "*** Dry run: disks not modified"
            echo
        else
            modify_output=$(aws ec2 modify-volume --volume-id $vol --size $new_root_volume_size)
            if [ ! $? -eq 0 ]; then
                error_and_exit An error occurred when trying to modify volume
            fi
        fi

        log result of modification: $modify_output
        message "${TAB}${TAB}root volume $vol set to $new_root_volume_size_text"
        message
        shift
    done
}

get_launch_template_version() {
    local autoscaling_group="$1"
    local launch_template_version=$(cat $DESCRIBE_AUTOSCALING_GROUP_FILE  | jq -r --arg ASG_NAME $autoscaling_group '.AutoScalingGroups[] | select(.AutoScalingGroupName == $ASG_NAME) | .MixedInstancesPolicy.LaunchTemplate.LaunchTemplateSpecification.Version')
    if [ "$launch_template_version" == "null" ]; then
        launch_template_version=$(cat $DESCRIBE_AUTOSCALING_GROUP_FILE | jq -r --arg ASG_NAME $autoscaling_group '.AutoScalingGroups[] | select(.AutoScalingGroupName == $ASG_NAME) | .LaunchTemplate.Version')
    fi

    if [ "$launch_template_version" == "null" ]; then
        error_and_exit could not retrieve launch template version for autoscaling group $autoscaling_group
    fi

    log launch template version is $launch_template_version
    echo $launch_template_version
}

get_launch_template_id() {
    local autoscaling_group="$1"
    local launch_template_id=$(cat $DESCRIBE_AUTOSCALING_GROUP_FILE  | \
        jq -r --arg ASG_NAME $autoscaling_group '.AutoScalingGroups[] | select(.AutoScalingGroupName == $ASG_NAME) | .MixedInstancesPolicy.LaunchTemplate.LaunchTemplateSpecification.LaunchTemplateId')

    if [ "$launch_template_id" == "null" ]; then
        launch_template_id=$(cat $DESCRIBE_AUTOSCALING_GROUP_FILE | jq -r --arg ASG_NAME $autoscaling_group '.AutoScalingGroups[] | select(.AutoScalingGroupName == $ASG_NAME) | .LaunchTemplate.LaunchTemplateId')
    fi

    if [ "$launch_template_id" == "null" ]; then
        error_and_exit could not retrieve launch template id for autoscaling group $autoscaling_group
    fi

    log launch template id is $launch_template_id
    echo $launch_template_id
}

create_new_launch_template_version() {
    local lt_id=$1
    local lt_latest_version=$2
    local new_root_volume_size=$3
    local latest_launch_template_version="$4"
    log "latest launch template version: $latest_launch_template_version"
    request_json=$(echo $latest_launch_template_version | jq --arg new_root_volume_size $new_root_volume_size  '.LaunchTemplateVersions[0] | .LaunchTemplateData|.BlockDeviceMappings[0].Ebs.VolumeSize |= ($new_root_volume_size | tonumber)')
    log created request json: "$request_json"

    if [ "$DRY_RUN" = true ]; then
        echo "*** Dry run: new launch template version not created"
        echo
    else
        created_launch_template=$(aws ec2 create-launch-template-version \
            --launch-template-id  $lt_id \
            --version-description "vertical scaling of root disks" \
            --source-version $lt_latest_version \
            --launch-template-data "$request_json")
        if [ $? -ne 0 ]; then
            error_and_exit modifying launch template $lt_id failed. Response from AWS: "$created_launch_template"
        fi
    fi

    log created launch template: $created_launch_template
    created_launch_template_version=$(echo $created_launch_template | jq '.LaunchTemplateVersion.VersionNumber')
    log created version: $created_launch_template_version

    echo $created_launch_template_version
}

update_autoscaling_group_with_launch_template_version() {
    local auto_scaling_group_name=$1
    local launch_template_id=$2
    local created_launch_template_version=$3

    if [ "$DRY_RUN" = true ]; then
        echo "*** Dry run: autoscaling group not changed"
        echo
    else
        aws autoscaling update-auto-scaling-group \
            --auto-scaling-group-name $asg \
            --launch-template LaunchTemplateId=$launch_template_id,Version=$created_launch_template_version
        if [ $? -ne 0 ]; then 
            error_and_exit modifying autoscaling group $autoscaling_group_id failed
        fi
    fi

}

main() {
    greetings
    log Invocation: $0 $*

    # parse parameters
    new_root_volume_size=$1
    new_root_volume_size_text="$new_root_volume_size GB"
    if [ -z "$new_root_volume_size" ]; then
        show_usage
        error_and_exit_no_log TARGET_ROOT_VOLUME_SIZE is missing
    fi

    instance_id_list=""
    shift 1
    while [ ! -z "$1" ]; do
        instance_id_list="$instance_id_list $1"
        shift
    done

    if [ -z "$instance_id_list" ]; then
        show_usage
        error_and_exit_no_log INSTANCE_ID_LIST is missing
    fi

    message
    message Will set root volume to $new_root_volume_size_text on instances $instance_id_list and their launch templates
    message

    if [ "$DRY_RUN" = true ]; then
        echo "========="
        echo "Running in DRY_RUN mode, nothing will be changed"
        echo "========="
        echo
    fi

    check_prerequisites

    # describe all instances
    message
    message 2. Checking instances
    log get info of instance
    describe_instances
    validate_instances_are_stopped

    # find root volumes of instances
    message 3. Changing root volume size in the instances

    root_volume_ids=$(get_root_volume_of_instances)
    log found root volume ids: "$root_volume_ids"

    # describe volumes, and get those where the size is smaller than the desired size
    aws ec2 describe-volumes --volume-ids $root_volume_ids > $DESCRIBE_VOLUME_FILE
    show_root_volume_to_size_map
    root_volumes_to_set=$(cat $DESCRIBE_VOLUME_FILE | jq -r --arg VOLUME_SIZE "$new_root_volume_size" '.Volumes[] | select(.Size < ($VOLUME_SIZE|tonumber)) | .VolumeId' | tr '\n' ' ')

    if [ -z "$root_volumes_to_set" ]; then
        message
        message RESULT: No root volumes found where the size is smaller than $new_root_volume_size_text.
    else
        message "    root volumes where the size is smaller than $new_root_volume_size_text: $root_volumes_to_set"

        # modify volumes where the root disk size is maller than desired
        set_root_volumes $new_root_volume_size "$new_root_volume_size_text" $root_volumes_to_set
        message "    The required volumes were increased to $new_root_volume_size_text."
    fi

    # modify the launch template -- will update launch tempalte of all provided instances, regardless if its root disk is already big enough
    message
    message
    message 4. Changing root volume size in the launch templates
    message
    all_autoscaling_groups=$(cat $DESCRIBE_INSTANCE_FILE | jq -r '.Reservations[] | .Instances[] | .Tags[] | select(.Key == "aws:autoscaling:groupName") | .Value' | sort | uniq | tr '\n' ' ')
    log autoscaling groups: $all_autoscaling_groups
    aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names $all_autoscaling_groups > $DESCRIBE_AUTOSCALING_GROUP_FILE

    for asg in $all_autoscaling_groups; do
        message "${TAB}Updating autoscaling group $asg to use new root volume size $new_root_volume_size"
        lt_id=$(get_launch_template_id $asg)
        lt_latest_version=$(get_launch_template_version $asg)
        message "${TAB}${TAB}Autoscaling group $asg uses launch template $lt_id with current version $lt_latest_version"

        aws ec2 describe-launch-template-versions --launch-template-id $lt_id --versions $lt_latest_version > latest-launch-template-version.json
        current_root_volume_size=$(cat latest-launch-template-version.json | jq '.LaunchTemplateVersions[0] | .LaunchTemplateData|.BlockDeviceMappings[0].Ebs.VolumeSize')
        message "${TAB}${TAB}The current root volume size is $current_root_volume_size GB"
        if [ $current_root_volume_size -lt $new_root_volume_size ]; then 
            latest_launch_template_version=$(aws ec2 describe-launch-template-versions --launch-template-id $lt_id --versions $lt_latest_version)
            log "latest launch template version: $latest_launch_template_version"
            created_launch_template_version=$(create_new_launch_template_version $lt_id $lt_latest_version $new_root_volume_size "$latest_launch_template_version")
            message "${TAB}${TAB}Created new launch template version, latest version: '$created_launch_template_version'"
            update_autoscaling_group_with_launch_template_version $asg $lt_id $created_launch_template_version
            message "${TAB}${TAB}Autoscaling group $asg was updated to the new version $created_launch_template_version"
        else
            message "${TAB}${TAB}Launch template $lt_id in autoscaling group $asg already has root disk size $new_root_volume_size_text, nothing changed"
        fi
        message
    done

    message
    message RESULT: Finished successfully. The required volumes were increased to $new_root_volume_size_text.
}

main $*
