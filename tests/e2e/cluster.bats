load ../commands
load ../resources
load ../parameters

DELAY=$(($SECONDS+2100))

@test "Check create credential - aws role based" {
  OUTPUT=$(create-credential-aws-role --name "${AWS_CREDENTIAL_NAME}" $AWS_ARGS_ROLE 2>&1 | tail -n 2 | head -n 1)
  [[ "${OUTPUT}" == *"credential created: ${AWS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Cluster create - aws" {
  OUTPUT=$(create-cluster --name $AWS_CLUSTER_NAME --cli-input-json $AWS_INPUT_JSON_FILE 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack created: ${AWS_CLUSTER_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Cluster create - wait for cluster is created" {
  run wait-cluster-status $DELAY $AWS_CLUSTER_NAME AVAILABLE
  [ $status = 0 ]
}

@test "Change ambari pwd" {
  OUTPUT=$(change-ambari-password --name "${AWS_CLUSTER_NAME}" --old-password admin --new-password 4321 --ambari-user admin 2>&1 | awk '{printf "%s",$0} END {print ""}' | grep -o '{.*}' | jq ' . |  [to_entries[].key] == ["oldPassword","password","userName"]')
  [[ "${OUTPUT}" ==  true ]]
}

@test "Cluster is listed" {
  for OUTPUT in $(list-clusters | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform","StackStatus","ClusterStatus"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
 }

@test "Cluster is described" {
  param="${AWS_CLUSTER_NAME}"
  export param
  OUTPUT=$(describe-cluster --name "${AWS_CLUSTER_NAME}" | jq '. "name"  == env.param' )
  [[ "$OUTPUT" == "true" ]]
}

@test "Stop cluster" {
  run stack-is-status $AWS_CLUSTER_NAME AVAILABLE
  if [ $status = 1 ] ; then
    skip "Cluster is not available skipping stop test"
  fi
  CHECK_RESULT=$( stop-cluster --name $AWS_CLUSTER_NAME )
  echo $CHECK_RESULT >&2
}

@test "Stop cluster - waiting for cluster is stopped" {
  run wait-stack-status $DELAY $AWS_CLUSTER_NAME STOPPED
  [ $status = 0 ]
}

@test "Start cluster" {
  run stack-is-status $AWS_CLUSTER_NAME STOPPED
  if [ $status = 1 ] ; then
    skip "Cluster is not stopped skipping start test"
  fi
  CHECK_RESULT=$( start-cluster --name $AWS_CLUSTER_NAME )
  echo $CHECK_RESULT >&2
}

@test "Start cluster - waiting for cluster is available" {
  run wait-cluster-status $DELAY $AWS_CLUSTER_NAME AVAILABLE
  [ $status = 0 ]
}

@test "Cluster upscaling" {
  skip
  CHECK_RESULT=$( describe-cluster --name $AWS_CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  INSTANCE_COUNT_DESIRED=$(($CHECK_RESULT + 1))
  echo $INSTANCE_COUNT_DESIRED > /tmp/clitestutil
  run scale-cluster --name $AWS_CLUSTER_NAME --group-name compute --desired-node-count $INSTANCE_COUNT_DESIRED
  [ $status = 0 ]
}

@test "Cluster upscaling - check cluster is available" {
  skip
  INSTANCE_COUNT_DESIRED=`cat /tmp/clitestutil`
  echo $INSTANCE_COUNT_DESIRED

  run wait-stack-cluster-status $DELAY $AWS_CLUSTER_NAME AVAILABLE
  status_available=$status

  run node-count-are-equal $AWS_CLUSTER_NAME

  INSTANCE_COUNT_CURRENT=$(describe-cluster --name $AWS_CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  echo $INSTANCE_COUNT_CURRENT

  STATUS_REASON=`cat /tmp/status_reason`
  echo $STATUS_REASON
  [ $status_available = 0 ] && [ $status = 0 ] && [ $INSTANCE_COUNT_DESIRED = $INSTANCE_COUNT_CURRENT ]
}

@test "Cluster downscaling" {
  skip
  CHECK_RESULT=$( describe-cluster --name $AWS_CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  INSTANCE_COUNT_DESIRED=$(($CHECK_RESULT - 1))
  echo $INSTANCE_COUNT_DESIRED > /tmp/clitestutil

  run scale-cluster --name $AWS_CLUSTER_NAME --group-name compute --desired-node-count $INSTANCE_COUNT_DESIRED
  echo $output
  [ $status = 0 ]
}

@test "Cluster downscaling - check cluster is available" {
  skip
  INSTANCE_COUNT_DESIRED=`cat /tmp/clitestutil`
  echo $INSTANCE_COUNT_DESIRED

  run wait-stack-cluster-status $DELAY $AWS_CLUSTER_NAME AVAILABLE
  status_available=$status

  run node-count-are-equal $AWS_CLUSTER_NAME

  INSTANCE_COUNT_CURRENT=$(describe-cluster --name $AWS_CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  echo $INSTANCE_COUNT_CURRENT

  STATUS_REASON=`cat /tmp/status_reason`
  echo $STATUS_REASON

  [ $status_available = 0 ] && [ $status = 0 ] && [ $INSTANCE_COUNT_DESIRED = $INSTANCE_COUNT_CURRENT ]
}

@test "Generate reinstall template" {
  OUTPUT=$(generate-reinstall-template --name "${AWS_CLUSTER_NAME}" --blueprint-name "${BLUEPRINT_NAME}" | jq .blueprintName -r)
  echo $OUTPUT
  [[ "${OUTPUT}" == "${BLUEPRINT_NAME}" ]]
}

@test "Teardown: delete cluster, credential" {
  CHECK_RESULT=$( delete-cluster --name "${AWS_CLUSTER_NAME}")
  echo $CHECK_RESULT >&2

  run wait-cluster-delete $DELAY "${AWS_CLUSTER_NAME}"
  [ $status = 0 ]

  CHECK_RESULT=$( delete-credential --name "${AWS_CREDENTIAL_NAME}" )
  echo $CHECK_RESULT >&2
}

@test "Teardown: delete testutil" {
  CHECK_RESULT=$( rm -f /tmp/status_reason )
  echo $CHECK_RESULT >&2

  CHECK_RESULT=$( rm -f /tmp/clitestutil )
  echo $CHECK_RESULT >&2
}