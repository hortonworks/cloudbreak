#!/usr/bin/env bats
load ../commands
load ../parameters

@test "Check create credential - aws role based" {
  OUTPUT=$(create-credential-aws-role --name ${TEST_CREDENTIAL_NAME} $AWS_ARGS_ROLE 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential created: ${TEST_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check credentials are listed" {
  for OUTPUT in $(list-credentials  | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check credential is described" {
  OUTPUT=$(describe-credential --name "${TEST_CREDENTIAL_NAME}" | jq .Name -r)

  [[ "${OUTPUT}" == "${TEST_CREDENTIAL_NAME}" ]]
}

@test "Check availability zone list" {
  OUTPUT=$(availability-zone-list --credential "${TEST_CREDENTIAL_NAME}" --region "${AWS_REGION}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${TEST_CREDENTIAL_NAME}" ]]
}

@test "Check regions are listed" {
  OUTPUT=$(region-list --credential "${TEST_CREDENTIAL_NAME}" |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' )
  [[ "$OUTPUT" == "true" ]]
}

@test "Check instances are listed" {
  OUTPUT=$(instance-list --credential "${TEST_CREDENTIAL_NAME}" --region "${AWS_REGION}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName","region"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${TEST_CREDENTIAL_NAME}" ]]
  [[ $(echo "${OUTPUT}" | jq -r ."region") == ${AWS_REGION} ]]
}

@test "Check volumes are listed - aws" {
  for OUTPUT in $(volume-list aws | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check volumes are listed - azure" {
  for OUTPUT in $(volume-list azure | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check volumes are listed - gcp" {
  for OUTPUT in $(volume-list gcp | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Check credential delete" {
  OUTPUT=$(delete-credential --name "${TEST_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"credential deleted, name: ${TEST_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}