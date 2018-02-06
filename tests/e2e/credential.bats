#!/usr/bin/env bats

load ../utils/commands

@test "Create new OpenStack V2 credential" {
  run remove-stuck-credential "${OS_CREDENTIAL_NAME}"

  OUTPUT=$(create-credential-openstack-v2 $OS_ARGS_V2 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential created: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Previously OpenStack V2 credential is listed" {
  for OUTPUT in $(list-credentials  | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Previously OpenStack V2 credential is described" {
  OUTPUT=$(describe-credential --name "${OS_CREDENTIAL_NAME}" | jq .Name -r)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == "${OS_CREDENTIAL_NAME}" ]]
}

@test "Previously OpenStack V2 credential is deleted" {
  OUTPUT=$(delete-credential --name "${OS_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential deleted, name: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}