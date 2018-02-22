#!/usr/bin/env bash

load ../utils/e2e_parameters
load ../utils/commands

@test "Check recipe create from url pre-ambari-start" {
  OUTPUT=$(create-recipe from-url --name ${RECIPE_NAME} --execution-type pre-ambari-start --url ${RECIPE_URL} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == PRE_AMBARI_START ]]
}

@test "Check recipes are listed" {
  for OUTPUT in $(list-recipes | jq ' .[] | [to_entries[].key] == ["Name","Description","ExecutionType"]');
  do
    [[ "$OUTPUT" == true ]]
  done
}

@test "Check recipe is described" {
  OUTPUT=$( describe-recipe --name ${RECIPE_NAME} | jq ' . | [to_entries[].key] == ["Name","Description","ExecutionType"]')

  [[ "$OUTPUT" == true ]]
}

@test "Check recipe delete" {
  OUTPUT=$(delete-recipe --name ${RECIPE_NAME} 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"recipe deleted, name: ${RECIPE_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
