load ../commands
load ../resources

AWS_CRED_NAME=testcred
REGION=eu-west-1

@test "Check availability zone list" {
  CHECK_RESULT=$( availability-zone-list --credential testcred --region testregion )
  echo $CHECK_RESULT >&2
}

 @test "Check regions are listed" {
  CHECK_RESULT=$( region-list --credential $AWS_CRED_NAME )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description"]' ) == true ]
}

@test "Check instances are listed" {
  run instance-list --credential $AWS_CRED_NAME --region $REGION
  [ $status = 1 ]
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
