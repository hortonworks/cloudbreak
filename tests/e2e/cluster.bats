load ../commands
load ../resources
load ../parameters

AWS_CRED_NAME=cli-cred-aws
AWS_ARGS_ROLE=" --name $AWS_CRED_NAME --role-arn $AWS_ROLE_ARN "
CLUSTER_NAME=cli-aws
DELAY=$(($SECONDS+2100))
INPUT_JSON_FILE=aws-template.json
REGION=eu-west-1
: ${BLUEPRINT_NAME:="EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0"}

@test "Check create credential - aws role based" {
  run create-credential-aws-role $AWS_ARGS_ROLE
  echo $output
  [ $status = 0 ]
}

@test "Check availability zone list" {
  CHECK_RESULT=$( availability-zone-list --credential $AWS_CRED_NAME --region $REGION )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name"]' ) == true ]
}

 @test "Check regions are listed" {
  CHECK_RESULT=$( region-list --credential $AWS_CRED_NAME )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' ) == true ]
}

@test "Check instances are listed" {
  CHECK_RESULT=$( instance-list --credential $AWS_CRED_NAME --region $REGION)
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Cpu","Memory","AvailabilityZone"]' ) == true ]
}

@test "Check volumes are listed - aws" {
  CHECK_RESULT=$( volume-list aws )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' ) == true ]
}

@test "Check volumes are listed - azure" {
  CHECK_RESULT=$( volume-list azure )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' ) == true ]
}

@test "Check volumes are listed - gcp" {
  CHECK_RESULT=$( volume-list gcp )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' ) == true ]
}

@test "Cluster create - aws" {
  run create-cluster --name $CLUSTER_NAME --cli-input-json $INPUT_JSON_FILE
  echo $output
  [ $status = 0 ]
}

@test "Cluster create - wait for cluster is created" {
  run wait-cluster-status $DELAY $CLUSTER_NAME AVAILABLE
  [ $status = 0 ]
}

@test "Change ambari pwd" {
  run change-ambari-password --name $CLUSTER_NAME --old-password admin --new-password admin2 --ambari-user admin
  echo $output
  [ $status = 0 ] && [ $output = ""]
}

@test "Cluster is listed" {
  [[ $(list-clusters | jq ' .[0] | [to_entries[].key] == ["Name","Description","CloudPlatform","StackStatus","ClusterStatus"]' ) == "true" ]]
}

@test "Cluster is described" {
  param=$CLUSTER_NAME
  export param
  CHECK_RESULT=$( describe-cluster --name $CLUSTER_NAME)
  [ $(echo $CHECK_RESULT | jq '. "name"  == env.param' ) == "true" ]
}

@test "Stop cluster" {
  run stack-is-status $CLUSTER_NAME AVAILABLE
  if [ $status = 1 ] ; then
    skip "Cluster is not available skipping stop test"
  fi
  CHECK_RESULT=$( stop-cluster --name $CLUSTER_NAME )
  echo $CHECK_RESULT >&2
}

@test "Stop cluster - waiting for cluster is stopped" {
  run wait-stack-status $DELAY $CLUSTER_NAME STOPPED
  [ $status = 0 ]
}

@test "Start cluster" {
  run stack-is-status $CLUSTER_NAME STOPPED
  if [ $status = 1 ] ; then
    skip "Cluster is not stopped skipping start test"
  fi
  CHECK_RESULT=$( start-cluster --name $CLUSTER_NAME )
  echo $CHECK_RESULT >&2
}

@test "Start cluster - waiting for cluster is available" {
  run wait-cluster-status $DELAY $CLUSTER_NAME AVAILABLE
  [ $status = 0 ]
}

@test "Cluster upscaling" {
  skip
  CHECK_RESULT=$( describe-cluster --name $CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  INSTANCE_COUNT_DESIRED=$(($CHECK_RESULT + 1))
  echo $INSTANCE_COUNT_DESIRED > /tmp/clitestutil
  run scale-cluster --name $CLUSTER_NAME --group-name compute --desired-node-count $INSTANCE_COUNT_DESIRED
  [ $status = 0 ]
}

@test "Cluster upscaling - check cluster is available" {
  skip
  INSTANCE_COUNT_DESIRED=`cat /tmp/clitestutil`
  echo $INSTANCE_COUNT_DESIRED

  run wait-stack-cluster-status $DELAY $CLUSTER_NAME AVAILABLE
  status_available=$status

  run node-count-are-equal $CLUSTER_NAME

  INSTANCE_COUNT_CURRENT=$(describe-cluster --name $CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  echo $INSTANCE_COUNT_CURRENT

  STATUS_REASON=`cat /tmp/status_reason`
  echo $STATUS_REASON
  [ $status_available = 0 ] && [ $status = 0 ] && [ $INSTANCE_COUNT_DESIRED = $INSTANCE_COUNT_CURRENT ]
}

@test "Cluster downscaling" {
  skip
  CHECK_RESULT=$( describe-cluster --name $CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  INSTANCE_COUNT_DESIRED=$(($CHECK_RESULT - 1))
  echo $INSTANCE_COUNT_DESIRED > /tmp/clitestutil

  run scale-cluster --name $CLUSTER_NAME --group-name compute --desired-node-count $INSTANCE_COUNT_DESIRED
  echo $output
  [ $status = 0 ]
}

@test "Cluster downscaling - check cluster is available" {
  skip
  INSTANCE_COUNT_DESIRED=`cat /tmp/clitestutil`
  echo $INSTANCE_COUNT_DESIRED

  run wait-stack-cluster-status $DELAY $CLUSTER_NAME AVAILABLE
  status_available=$status

  run node-count-are-equal $CLUSTER_NAME

  INSTANCE_COUNT_CURRENT=$(describe-cluster --name $CLUSTER_NAME | jq ' ."instanceGroups" | .[] | select(."group"=="compute") | . "nodeCount" ')
  echo $INSTANCE_COUNT_CURRENT

  STATUS_REASON=`cat /tmp/status_reason`
  echo $STATUS_REASON

  [ $status_available = 0 ] && [ $status = 0 ] && [ $INSTANCE_COUNT_DESIRED = $INSTANCE_COUNT_CURRENT ]

}

@test "Generate reinstall template" {
  CHECK_RESULT=$( generate-reinstall-template --name $CLUSTER_NAME --blueprint-name "${BLUEPRINT_NAME}" | jq .blueprintName -r)
  echo $CHECK_RESULT
  [[ "${CHECK_RESULT}" == "${BLUEPRINT_NAME}" ]]
}

@test "Cluster is synced" {
  run sync-cluster --name $CLUSTER_NAME
  echo $output
  [ $status = 0 ]
}

@test "Teardown: delete cluster, credential" {
  CHECK_RESULT=$( delete-cluster --name $CLUSTER_NAME )
  echo $CHECK_RESULT >&2

  run wait-cluster-delete $DELAY $CLUSTER_NAME
  [ $status = 0 ]

  CHECK_RESULT=$( delete-credential --name $AWS_CRED_NAME )
  echo $CHECK_RESULT >&2
}

@test "Teardown: delete testutil" {
  CHECK_RESULT=$( rm -f /tmp/status_reason )
  echo $CHECK_RESULT >&2

  CHECK_RESULT=$( rm -f /tmp/clitestutil )
  echo $CHECK_RESULT >&2
}