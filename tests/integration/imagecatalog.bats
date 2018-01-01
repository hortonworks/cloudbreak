load ../commands

@test "List imagecatalog" {
  CHECK_RESULT=$( list-image-catalog $AWS_ARGS_ROLE )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Default","URL"]' ) == true ]
}

@test "Create image catalog" {
  CHECK_RESULT=$( create-image-catalog --name test --url test )
  echo $CHECK_RESULT >&2
}

@test "Delete image catalog" {
  CHECK_RESULT=$( delete-image-catalog --name test )
  echo $CHECK_RESULT >&2
}

@test "Get images - openstack" {
  CHECK_RESULT=$( get-images openstack --imagecatalog a )
  echo $CHECK_RESULT >&2
}

@test "Set default image catalog" {
  CHECK_RESULT=$( set-default-image-catalog --name test )
  echo $CHECK_RESULT >&2
}