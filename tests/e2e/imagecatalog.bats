load ../commands

IMAGE_CATALOG_NAME=cli-ic-5
IMAGE_CATALOG_NAME_ORIG=cloudbreak-default
IMAGE_CATALOG_URL=https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json

@test "Create image catalog" {
  CHECK_RESULT=$( create-image-catalog --name $IMAGE_CATALOG_NAME --url $IMAGE_CATALOG_URL )
  echo $CHECK_RESULT >&2
}

@test "Get images - openstack" {
  CHECK_RESULT=$( get-images openstack --imagecatalog $IMAGE_CATALOG_NAME )
  echo $CHECK_RESULT >&2
}

@test "Set default image catalog" {
  CHECK_RESULT=$( set-default-image-catalog --name $IMAGE_CATALOG_NAME )
  echo $CHECK_RESULT >&2
}

@test "Set original image catalog back" {
  CHECK_RESULT=$( set-default-image-catalog --name $IMAGE_CATALOG_NAME_ORIG )
  echo $CHECK_RESULT >&2
}

@test "List image catalog" {
  CHECK_RESULT=$( list-image-catalog )
  [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Default","URL"]' ) == true ]
}

@test "Delete image catalog" {
  CHECK_RESULT=$( delete-image-catalog --name $IMAGE_CATALOG_NAME )
  echo $CHECK_RESULT >&2
}