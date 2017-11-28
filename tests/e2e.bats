load commands

@test "Check credentials are listed" {
  CHECK_RESULT=$( list-credentials )
  [ $(echo $CHECK_RESULT |  jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform"]' ) == true ]
}
