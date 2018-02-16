#!/usr/bin/env bash

load ../utils/commands

@test "Create new image catalog with DEV URL" {
  OUTPUT=$(create-image-catalog --name "${IMAGE_CATALOG_NAME}" --url "${IMAGE_CATALOG_URL}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"imagecatalog created"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Get images - openstack" {
  for OUTPUT in $(get-images openstack --imagecatalog "${IMAGE_CATALOG_NAME}" | jq ' .[0] | [to_entries[].key] == ["Date","Description","Version","ImageID"]');
  do
    [[ "$OUTPUT" == true ]]
  done
}

@test "Set default image catalog" {
  OUTPUT=$(set-default-image-catalog --name "${IMAGE_CATALOG_NAME}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | grep -o '{.*}' | jq ' . | [to_entries[].key] == ["name","url","id","publicInAccount","usedAsDefault"]')

  [[ "${OUTPUT}" ==  true ]]
}

@test "Set original image catalog back" {
  OUTPUT=$(set-default-image-catalog --name "${IMAGE_CATALOG_NAME_DEFAULT}" 2>&1 | awk '{printf "%s",$0} END {print ""}' | grep -o '{.*}' | jq ' . | [to_entries[].key] == ["name","url","publicInAccount","usedAsDefault"]')

  [[ "${OUTPUT}" ==  true ]]
}

@test "List image catalog" {
  for OUTPUT in $(list-image-catalog  | jq ' .[0] | [to_entries[].key] == ["Name","Default","URL"]');
  do
    [[ "$OUTPUT" == true ]]
  done
}

@test "Delete image catalog" {
  OUTPUT=$(delete-image-catalog --name "${IMAGE_CATALOG_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"imagecatalog deleted, name: ${IMAGE_CATALOG_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}