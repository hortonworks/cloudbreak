function wait-stack-status() {
    param1=$3
    export param1
    is_status=false
    while [ $SECONDS -lt $1 ] && [ $is_status == false ]
    do
	sleep 30
	is_status=$(describe-cluster --name $2 | jq -r '."status"==env.param1')
    done
    [ $is_status == true ]
}

function stack-is-status() {
    param1=$2
    export param1
    is_status=false
	is_status=$(describe-cluster --name $1 | jq -r '."status"==env.param1')
	[ $is_status == true ]
}

function wait-cluster-status() {
    param1=$3
    export param1
    is_status=false
    while [ $SECONDS -lt $1 ] && [ $is_status == false ]
    do
	sleep 30
	is_status=$(describe-cluster --name $2 | jq -r ' ."cluster" | ."status"==env.param1')
    done
    [ $is_status == true ]
}

function cluster-is-status() {
    param1=$2
    export param1
    is_status=false
	is_status=$(describe-cluster --name $1 | jq -r ' ."cluster" | ."status"==env.param1')
    [ $is_status == true ]
}

function node-count-are-equal() {
  NODES_COUNT_CLUSTER=$( describe-cluster --name $1 | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  NODES_COUNT_STACK=$( describe-cluster --name $1 | jq ' ."cluster"  | . "hostGroups" | . [] | . "constraint" | select(."instanceGroupName" == "compute") | . "hostCount"')
  [ $NODES_COUNT_CLUSTER = $NODES_COUNT_STACK ]
}

function wait-cluster-delete() {
    param1=$2
    export param1
    is_status=false
    while [ $SECONDS -lt $1 ] && [ $is_status == false ]
    do
	sleep 30
    tmp=$(list-clusters | jq '.[] |  select(."Name" == env.param1) ' )
	if [$tmp == ""] ; then
	   is_status=true
	fi
    done
    echo $is_status
    [ $is_status == true ]
}

