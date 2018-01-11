#!/usr/bin/env bats

load ../commands
load ../parameters

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
  OUTPUT=$(describe-cluster --name "${CLUSTER_NAME}" | jq .name -r)

  [[ "${OUTPUT}" == "${CLUSTER_NAME}" ]]
}

@test "Check cluster describe FAILED" {
  OUTPUT=$(DEBUG=1 describe-cluster --name az404 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'az404' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster delete SUCCESS" {
  OUTPUT=$(DEBUG=1 cb cluster delete --name "${CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack deleted, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster delete FAILED" {
  OUTPUT=$(DEBUG=1 cb cluster delete --name az404 2>&1 | tail -n 3 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 404, message: Stack 'az404' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster start SUCCESS" {
  OUTPUT=$(DEBUG=1 start-cluster --name "${CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack started, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster start FAILED" {
  OUTPUT=$(DEBUG=1 start-cluster --name azstatus 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check clusters top SUCCESS" {
  OUTPUT=$(DEBUG=1 stop-cluster --name "${CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack stopted, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster stop FAILED" {
  OUTPUT=$(DEBUG=1 stop-cluster --name azstatus 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster sync SUCCESS" {
  OUTPUT=$(DEBUG=1 sync-cluster --name "${CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack synced, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster sync FAILED" {
  OUTPUT=$(DEBUG=1 sync-cluster --name azstatus 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster repair SUCCESS" {
  OUTPUT=$(DEBUG=1 repair-cluster --name "${CLUSTER_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack repaired, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster repair FAILED" {
  OUTPUT=$(DEBUG=1 repair-cluster --name azstatus 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster scale SUCCESS" {
  OUTPUT=$(DEBUG=1 scale-cluster --name "${CLUSTER_NAME}" --group-name worker --desired-node-count 5 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack scaled, name: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster scale FAILED" {
  OUTPUT=$(DEBUG=1 scale-cluster --name azstatus --group-name worker --desired-node-count 6 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'azstatus' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create SUCCESS" {
  OUTPUT=$(DEBUG=1 create-cluster --cli-input-json template.json --name aaaaa 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack created: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster create without name" {
  OUTPUT=$(DEBUG=1 create-cluster --cli-input-json template.json 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"Name of the cluster must be set either in the template or with the --name command line option."* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create without password" {
  skip "BUG-94445"

  OUTPUT=$(DEBUG=1 create-cluster --cli-input-json template_wo_pwd.json --name aaaaa 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: ambariRequest password may not be null"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check cluster create without password by parameter" {
  OUTPUT=$(DEBUG=1 create-cluster --cli-input-json template_wo_pwd.json --name aaaaa --input-json-param-password 1234 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"stack created: openstack-cluster"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check cluster re-install" {
  skip "BUG-94446"

  OUTPUT=$(DEBUG=1 reinstall-cluster --name test --cli-input-json template.json --name aaaaa 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"status code: 400, message: Stack 'aaaaa' not found"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}