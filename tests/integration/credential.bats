load ../commands

COMMON_ARGS_WO_CLUSTER=" --server ${CLOUD_URL} --username ${EMAIL} --password ${PASSWORD}  "

AWS_ARGS_KEY=" --name cli-aws-key --access-key testaccess --secret-key testsecretkey "
AWS_ARGS_ROLE=" --name cli-aws-role --role-arn  testawsrole "
OPENSTACK_ARGS=" --name cli-openstack --tenant-user testuser  --tenant-password testpassword --tenant-name testtenant --endpoint http://1.1.1.1:5000/v2.0"
GCP_ARGS=" --name cli-gcp --project-id testprojet --service-account-id testuser@siq-haas.iam.gserviceaccount.com --service-account-private-key-file test.p12"
AZURE_ARGS=" --name cli-azure --subscription-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --tenant-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --app-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --app-password testpassword"

@test "Check configure" {
  CHECK_RESULT=$( configure-cb $COMMON_ARGS_WO_CLUSTER)
  echo $CHECK_RESULT >&2
}

@test "Check create aws credential role based" {
  echo $AWS_ARGS_ROLE
  CHECK_RESULT=$( create-credential-aws-role $AWS_ARGS_ROLE )
  echo $CHECK_RESULT >&2
}

@test "Check create aws credential key based" {
  CHECK_RESULT=$( create-credential-aws-key $AWS_ARGS_KEY )
  echo $CHECK_RESULT >&2
}

@test "Check create azure credential" {
  CHECK_RESULT=$( create-credential-azure $AZURE_ARGS )
  echo $CHECK_RESULT >&2
}

@test "Check create openstack credential" {
  CHECK_RESULT=$( create-credential-openstack-v2 $OPENSTACK_ARGS )
  echo $CHECK_RESULT >&2
}

@test "Check create openstack credential" {
  CHECK_RESULT=$( create-credential-openstack-v3 $OPENSTACK_ARGS )
  echo $CHECK_RESULT >&2
}

@test "Check create gcp credential" {
  CHECK_RESULT=$( create-credential-gcp $GCP_ARGS )
  echo $CHECK_RESULT >&2
}

@test "Check delete credential" {
  CHECK_RESULT=$( delete-credential --name testcred )
  echo $CHECK_RESULT >&2
}

@test "Check credentials are listed" {
  CHECK_RESULT=$( list-credentials )
  [ $(echo $CHECK_RESULT |  jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]' ) == true ]
}

@test "Check credential is described" {
  CHECK_RESULT=$( describe-credential --name testcred)
  [ $(echo $CHECK_RESULT |  jq ' . | [to_entries[].key] == ["Name","Description","CloudPlatform"]' ) == true ]
}