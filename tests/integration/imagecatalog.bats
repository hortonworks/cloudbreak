#!/usr/bin/env bats

load ../commands

@test "List imagecatalog" {
  for OUTPUT in $(list-image-catalog  | jq ' .[0] | [to_entries[].key] == ["Name","Default","URL"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Create image catalog" {
  OUTPUT=$(create-image-catalog --name test --url test 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"imagecatalog created"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Delete image catalog" {
  OUTPUT=$(delete-image-catalog --name test 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"imagecatalog deleted, name: test"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Get images - openstack" {
  for OUTPUT in $(get-images openstack --imagecatalog a  | jq ' .[0] | [to_entries[].key] == ["Date","Description","Version","ImageID"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "Set default image catalog" {
  OUTPUT=$(set-default-image-catalog --name test 2>&1 | awk '{printf "%s",$0} END {print ""}' | grep -o '{.*}' | jq ' . | [to_entries[].key] == ["default","publicInAccount","name","id","url"]')
  [[ "${OUTPUT}" ==  true ]]
}