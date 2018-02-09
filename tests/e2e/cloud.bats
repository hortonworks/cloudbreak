#!/usr/bin/env bats

load ../utils/commands
load ../utils/resources

@test "PRECONDITION: Create new ["${OS_CREDENTIAL_NAME}"] OpenStack V2 credential" {
  run remove-stuck-credential "${OS_CREDENTIAL_NAME}"
  echo "$output" >&2

  OUTPUT=$(create-credential-openstack-v2 $OS_ARGS_V2 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential created: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Availability zone is listed" {
  OUTPUT=$(availability-zone-list --credential "${OS_CREDENTIAL_NAME}" --region "${OS_REGION}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) {print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${OS_CREDENTIAL_NAME}" ]]
}

@test "Regions are listed" {
  OUTPUT=$(cb cloud regions --credential "${OS_CREDENTIAL_NAME}" | jq '.[] | [to_entries[].key] == ["Name","Description"]')

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == "true" ]]
}

@test "Instances are listed" {
  OUTPUT=$(instance-list --credential "${OS_CREDENTIAL_NAME}" --region "${OS_REGION}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  echo "${OUTPUT}" >&2

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName","region"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${OS_CREDENTIAL_NAME}" ]]
  [[ $(echo "${OUTPUT}" | jq -r ."region") == "${OS_REGION}" ]]
}

@test "AWS volumes are listed" {
  for OUTPUT in $(volume-list aws | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "${OUTPUT}" == "true" ]]
  done
}

@test "Azure volumes are listed" {
  for OUTPUT in $(volume-list azure | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "${OUTPUT}" == "true" ]]
  done
}

@test "GCP volumes are listed" {
  for OUTPUT in $(volume-list gcp | jq ' .[0] | [to_entries[].key] == ["Name","Description"]');
  do
    [[ "${OUTPUT}" == "true" ]]
  done
}

@test "TEARDOWN: Delete ["${OS_CREDENTIAL_NAME}"] OpenStack credential" {
  OUTPUT=$(delete-credential --name "${OS_CREDENTIAL_NAME}" 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"credential deleted, name: ${OS_CREDENTIAL_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}