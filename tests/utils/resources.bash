#!/usr/bin/env bash

# Clusbreak statuses are implemented as:
# https://github.com/hortonworks/cloudbreak/blob/master/core-api/src/main/java/com/sequenceiq/cloudbreak/api/model/Status.java
# REQUESTED,
# CREATE_IN_PROGRESS,
# AVAILABLE,
# UPDATE_IN_PROGRESS,
# UPDATE_REQUESTED,
# UPDATE_FAILED,
# CREATE_FAILED,
# ENABLE_SECURITY_FAILED,
# PRE_DELETE_IN_PROGRESS,
# DELETE_IN_PROGRESS,
# DELETE_FAILED,
# DELETE_COMPLETED,
# STOPPED,
# STOP_REQUESTED,
# START_REQUESTED,
# STOP_IN_PROGRESS,
# START_IN_PROGRESS,
# START_FAILED,
# STOP_FAILED,
# WAIT_FOR_SYNC

function wait-cluster-status() {
    wait_cluster_name=$1
    cluster_status=${2:-AVAILABLE}
    is_status_available=false
    create_failed=false
    countup=0

    while [ "$countup" -lt 100 ] && [ "$is_status_available" == false ] && [ "$create_failed" == false ]
    do
    	countup=$(($countup+1))
    	sleep 30
	    if [[ $(cb cluster describe --name "$wait_cluster_name" | jq -r .status) == "$cluster_status" ]]; then
            is_status_available=true
        fi
	    if [[ $(cb cluster describe --name "$wait_cluster_name" | jq -r .cluster.status) == "CREATE_FAILED" ]]; then
            create_failed=true
        fi
    done

    if [[ "$is_status_available" != true ]]; then
        echo $(cb cluster describe --name "$wait_cluster_name" | jq -r .statusReason)
    else
        echo "$is_status_available"
    fi
}

function wait-stack-status() {
    wait_stack_name=$1
    stack_status=${2:-AVAILABLE}
    is_status_available=false
    create_failed=false
    countup=0

    while [ "$countup" -lt 100 ] && [ "$is_status_available" == false ] && [ "$create_failed" == false ]
    do
    	countup=$(($countup+1))
    	sleep 30
	    if [[ $(cb cluster describe --name "$wait_stack_name" | jq -r .cluster.status) == "$stack_status" ]]; then
            is_status_available=true
        fi
	    if [[ $(cb cluster describe --name "$wait_stack_name" | jq -r .cluster.status) == "CREATE_FAILED" ]]; then
            create_failed=true
        fi
    done

    if [[ "$is_status_available" != true ]]; then
        echo $(cb cluster describe --name "$wait_stack_name" | jq -r .statusReason)
    else
        echo "$is_status_available"
    fi
}

function is-cluster-status() {
    status_cluster_name=$1
    cluster_status=${2:-AVAILABLE}
    status_is=false

    if [[ $(cb cluster describe --name "$status_cluster_name" | jq -r .status) == "$cluster_status" ]]; then
        status_is=true
    fi

    if [[ "$status_is" != true ]]; then
        echo $(cb cluster describe --name "$status_cluster_name" | jq -r .statusReason)
    else
        echo "$status_is"
    fi
}

function is-stack-status() {
    status_stack_name=$1
    stack_status=${2:-AVAILABLE}
    status_is=false

    if [[ $(cb cluster describe --name "$status_stack_name" | jq -r .cluster.status) == "$stack_status" ]]; then
        status_is=true
    fi

    if [[ "$status_is" != true ]]; then
        echo $(cb cluster describe --name "$status_stack_name" | jq -r .statusReason)
    else
        echo "$status_is"
    fi
}

function wait-cluster-delete() {
    delete_cluster_name=$1
    still_exist=true
    delete_failed=false
    countup=0

    while [ "$countup" -lt 100 ] && [ "$still_exist" == true ] && [ "$delete_failed" == false ]
    do
    	countup=$(($countup+1))
    	sleep 30
	    if [[ $(cb cluster list | jq -r .[].Name) != "$delete_cluster_name" ]]; then
            still_exist=false
        fi
	    if [[ $(cb cluster describe --name "$delete_cluster_name" | jq -r .status) == "DELETE_FAILED" ]]; then
            delete_failed=true
        fi
    done

    if [[ "$still_exist" == true ]]; then
        echo $(cb cluster describe --name "$delete_cluster_name" | jq -r .statusReason)
    else
        echo "$still_exist"
    fi
}

function is-cluster-present() {
  present_cluster_name=$1

  if [[ $(cb cluster list | jq -r .[].Name) == "$present_cluster_name" ]]; then
    echo true
  fi
}

function remove-stuck-cluster() {
  remove_cluster_name=$1

  if [[ $(cb cluster list | jq -r .[].Name) == "$remove_cluster_name" ]]; then
    cb cluster delete --name "$remove_cluster_name" --force --wait
  fi
}

function remove-stuck-credential() {
  remove_credential_name=$1

  if [[ $(cb credential describe --name "$remove_credential_name" | jq -r .Name) == "$remove_credential_name" ]]; then
    cb credential delete --name "$remove_credential_name"
  fi
}

