#!/usr/bin/env bats

load ../utils/commands
load ../utils/resources

@test "SETUP: Cleanup stuck OpenStack ["${OS_CREDENTIAL_NAME}"] credential" {
  run remove-stuck-credential "${OS_CREDENTIAL_NAME}"

  echo "$output" >&2

  [ $status -eq 0 ]
}

@test "Create new ["${OS_CREDENTIAL_NAME}"] OpenStack V2 credential" {
  OUTPUT=$(create-credential-openstack-v2 --name "${OS_CREDENTIAL_NAME}" $OS_ARGS_V2 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential created: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "List ["${OS_CREDENTIAL_NAME}"] OpenStack V2 credential" {
  for OUTPUT in $(list-credentials  | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Describe ["${OS_CREDENTIAL_NAME}"] OpenStack V2 credential" {
  OUTPUT=$(describe-credential --name "${OS_CREDENTIAL_NAME}" | jq .Name -r)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == "${OS_CREDENTIAL_NAME}" ]]
}

@test "TEARDOWN: Delete ["${OS_CREDENTIAL_NAME}"] OpenStack credential" {
  OUTPUT=$(delete-credential --name "${OS_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential deleted, name: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
