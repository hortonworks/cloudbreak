load ../commands

AWS_CRED_NAME="cli-cred-aws-test"
AWS_ARGS_ROLE=" --name $AWS_CRED_NAME --role-arn $AWS_ROLE_ARN "

@test "Check create credential aws role based" {
  CHECK_RESULT=$( create-credential-aws-role $AWS_ARGS_ROLE )
  echo $CHECK_RESULT >&2
}

@test "Check credentials are listed" {
  CHECK_RESULT=$( list-credentials )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description","CloudPlatform"]' ) == true ]
}

@test "Check credential is described" {
  CHECK_RESULT=$( describe-credential --name $AWS_CRED_NAME )
  [ $(echo $CHECK_RESULT |  jq ' . | [to_entries[].key] == ["Name","Description","CloudPlatform"]' ) == true ]
}

@test "Check previously created credential" {
  CHECK_RESULT=$( list-credentials | grep $AWS_CRED_NAME)
  echo $CHECK_RESULT >&2
}

@test "Check delete credential" {
  CHECK_RESULT=$( delete-credential --name $AWS_CRED_NAME)
  echo $CHECK_RESULT >&2
}