load ../commands
load ../resources
load ../parameters

@test "Check availability zone list" {
  OUTPUT=$(availability-zone-list --credential "${OS_CREDENTIAL_NAME}" --region testregion 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${OS_CREDENTIAL_NAME}" ]]
}

 @test "Check regions are listed" {
  OUTPUT=$(region-list --credential "${OS_CREDENTIAL_NAME}" |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' )
  [[ "$OUTPUT" == "true" ]]
}

@test "Check instances are listed" {
  OUTPUT=$(instance-list --credential "${OS_CREDENTIAL_NAME}" --region testregion 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["credentialName","region"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."credentialName") == "${OS_CREDENTIAL_NAME}" ]]
  [[ $(echo "${OUTPUT}" | jq -r ."region") == testregion ]]
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