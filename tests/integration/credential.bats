#!/usr/bin/env bash

load ../commands
load ../parameters

@test "Check CB configure" {
  OUTPUT=$(DEBUG=1 configure-cb $COMMON_ARGS_WO_CLUSTER 2>&1 | sed -e '$!d')

  [[ "${OUTPUT}" == *"writing credentials to file:"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create AWS role based" {
  OUTPUT=$(DEBUG=1 create-credential-aws-role $AWS_ARGS_ROLE 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create AWS key based" {
  OUTPUT=$(DEBUG=1 create-credential-aws-key $AWS_ARGS_KEY 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create Azure" {
  OUTPUT=$(DEBUG=1 create-credential-azure $AZURE_ARGS 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create OpenStack V2" {
  OUTPUT=$(DEBUG=1 create-credential-openstack-v2 $OPENSTACK_ARGS_V2 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create OpenStack V3" {
  OUTPUT=$(DEBUG=1 create-credential-openstack-v3 $OPENSTACK_ARGS_V3 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential create GCP" {
  OUTPUT=$(DEBUG=1 create-credential-gcp $GCP_ARGS 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential delete" {
  OUTPUT=$(DEBUG=1 delete-credential --name "${OPENSTACK_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential deleted, name: ${OPENSTACK_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credential list" {
  for OUTPUT in $(list-credentials  | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]' );
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check credential described result" {
  OUTPUT=$(describe-credential --name "${OPENSTACK_CREDENTIAL_NAME}" | jq .Name -r)

  [[ "${OUTPUT}" == "${OPENSTACK_CREDENTIAL_NAME}" ]]
}

@test "Check credential described structure" {
  OUTPUT=$(describe-credential --name "${OPENSTACK_CREDENTIAL_NAME}" |  jq ' . | [to_entries[].key] == ["Name","Description","CloudPlatform"]')

  [[ "${OUTPUT}" == "true" ]]
}