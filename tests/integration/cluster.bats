#!/usr/bin/env bats

load ../utils/commands
load ../utils/mock_parameters

@test "Check cluster list" {
  CB_CLUSTERS=$(list-clusters | jq 'length')
  API_CLUSTERS=$(curl -ks "${BASE_URL}"/cb/api/v1/stacks/account | jq 'length')

  [[ CB_CLUSTERS -eq API_CLUSTERS ]]
}

@test "Check cluster list attributes" {
  for OUTPUT in $(list-clusters | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform","StackStatus","ClusterStatus"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check cluster describe result" {
  OUTPUT=$(describe-cluster --name "${OS_CLUSTER_NAME}" | jq .name -r)

  [[ "${OUTPUT}" == "${OS_CLUSTER_NAME}" ]]
}

@test "Check cluster describe FAILED" {
  OUTPUT=$(describe-cluster --name az404 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'az404' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster delete SUCCESS" {
  OUTPUT=$(delete-cluster "${OS_CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack deleted, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster delete FAILED" {
  OUTPUT=$(delete-cluster az404 2>&1 | tail -n 5 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'az404' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster start SUCCESS" {
  OUTPUT=$(start-cluster --name "${OS_CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack started, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster start FAILED" {
  OUTPUT=$(start-cluster --name azstatus 2>&1 | tail -n 4 | head -n 1)
  echo  $OUTPUT

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check clusters top SUCCESS" {
  OUTPUT=$(stop-cluster --name "${OS_CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack stopted, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster stop FAILED" {
  OUTPUT=$(stop-cluster --name azstatus 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster sync SUCCESS" {
  OUTPUT=$(sync-cluster --name "${OS_CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack synced, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster sync FAILED" {
  OUTPUT=$(sync-cluster --name azstatus 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster repair SUCCESS" {
  skip "The repairClusterV2 endpoint should be added to cb-mock. Waiting for the new cb-mock implementation."
  OUTPUT=$(repair-cluster --name "${OS_CLUSTER_NAME}" --host-groups worker 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack repaired, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster repair FAILED" {
  skip "The repairClusterV2 endpoint should be added to cb-mock. Waiting for the new cb-mock implementation."
  OUTPUT=$(repair-cluster --name azstatus --host-groups worker 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster scale SUCCESS" {
  OUTPUT=$(scale-cluster --name "${OS_CLUSTER_NAME}" --group-name worker --desired-node-count 5 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack scaled, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster scale FAILED" {
  OUTPUT=$(scale-cluster --name azstatus --group-name worker --desired-node-count 6 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create SUCCESS" {
  OUTPUT=$(create-cluster --cli-input-json templates/template.json --name aaaaa 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack created: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster create without name" {
  OUTPUT=$(create-cluster --cli-input-json templates/template.json 2>&1 | tail -n 3 | head -n 1)

  [[ "${OUTPUT}" == *"Name of the cluster must be set either in the template or with the --name command line option."* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create without password" {
  skip "BUG-94445"

  OUTPUT=$(create-cluster --cli-input-json templates/template_wo_pwd.json --name aaaaa 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: ambariRequest password may not be null"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create without password by parameter" {
  OUTPUT=$(create-cluster --cli-input-json templates/template_wo_pwd.json --name aaaaa --input-json-param-password 1234 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack created: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster re-install" {
  OUTPUT=$(reinstall-cluster --name test --cli-input-json templates/reinstall-template.json --name aaaaa 2>&1 | tail -n 4 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'aaaaa' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}
