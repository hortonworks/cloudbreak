load commands

@test "Check configure" {
  CHECK_RESULT=$( configure-cb )
  echo $CHECK_RESULT >&2
}

@test "Check create aws credential role based" {
  CHECK_RESULT=$( create-credential-aws-role )
  echo $CHECK_RESULT >&2
}

@test "Check create aws credential key based" {
  CHECK_RESULT=$( create-credential-aws-key )
  echo $CHECK_RESULT >&2
}

@test "Check create azure credential" {
  CHECK_RESULT=$( create-credential-azure )
  echo $CHECK_RESULT >&2
}

@test "Check create openstack credential" {
  CHECK_RESULT=$( create-credential-openstack-v2 )
  echo $CHECK_RESULT >&2
}

@test "Check create openstack credential" {
  CHECK_RESULT=$( create-credential-openstack-v3 )
  echo $CHECK_RESULT >&2
}

@test "Check create gcp credential" {
  CHECK_RESULT=$( create-credential-gcp )
  echo $CHECK_RESULT >&2
}

@test "Check delete openstack credential" {
  CHECK_RESULT=$( delete-credential --name testcred)
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