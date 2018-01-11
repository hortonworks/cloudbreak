#!/usr/bin/env bash

load ../commands
load ../parameters

@test "Check CB configure" {
  OUTPUT=$(configure-cb $COMMON_ARGS_WO_CLUSTER 2>&1 | sed -e '$!d')

  [[ "${OUTPUT}" == *"writing credentials to file:"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create AWS role based" {
  OUTPUT=$(create-credential-aws-role --name "${AWS_CREDENTIAL_NAME}" $AWS_ARGS_ROLE 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create AWS key based" {
  OUTPUT=$(create-credential-aws-key --name "${AWS_CREDENTIAL_NAME}" $AWS_ARGS_KEY 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create Azure" {
  OUTPUT=$(create-credential-azure $AZURE_ARGS 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create OpenStack V2" {
  OUTPUT=$(create-credential-openstack-v2 $OPENSTACK_ARGS_V2 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create OpenStack V3" {
  OUTPUT=$(create-credential-openstack-v3 $OPENSTACK_ARGS_V3 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create GCP" {
  OUTPUT=$(create-credential-gcp $GCP_ARGS 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential delete" {
  OUTPUT=$(delete-credential --name "${OS_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential deleted, name: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential list" {
  for OUTPUT in $(list-credentials  | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check credential described result" {
  OUTPUT=$(describe-credential --name "${OS_CREDENTIAL_NAME}" | jq .Name -r)

  [[ "${OUTPUT}" == "${OS_CREDENTIAL_NAME}" ]]
}

@test "Check credential described structure" {
  OUTPUT=$(describe-credential --name "${OS_CREDENTIAL_NAME}" |  jq ' . | [to_entries[].key] == ["Name","Description","CloudPlatform"]')

  [[ "${OUTPUT}" == "true" ]]
}