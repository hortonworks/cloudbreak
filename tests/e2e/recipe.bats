#!/usr/bin/env bash

load ../utils/e2e_parameters
load ../utils/commands

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