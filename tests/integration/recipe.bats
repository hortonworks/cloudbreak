load ../commands
load ../parameters

@test "Check recipe create from url pre-ambari-start" {
  OUTPUT=$(create-recipe from-url --name recipe --execution-type pre-ambari-start --url ${RECIPE_URL} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == PRE_AMBARI_START ]]
 }

@test "Check recipe create from url post-ambari-start" {
  OUTPUT=$(create-recipe from-url --name recipe --execution-type post-ambari-start --url ${RECIPE_URL} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == POST_AMBARI_START ]]
 }

@test "Check recipe create from url post-cluster-install" {
  OUTPUT=$(create-recipe from-url --name recipe --execution-type post-cluster-install --url ${RECIPE_URL} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == POST_CLUSTER_INSTALL ]]
 }

@test "Check recipe create from url pre-termination" {
  OUTPUT=$(create-recipe from-url --name recipe --execution-type pre-termination --url ${RECIPE_URL} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == PRE_TERMINATION ]]
 }

@test "Check recipe create from file pre-ambari-start" {
  OUTPUT=$(create-recipe from-file --name recipe --execution-type pre-ambari-start --file ${RECIPE_FILE} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == PRE_AMBARI_START ]]
 }

@test "Check recipe create from file post-ambari-start" {
  OUTPUT=$(create-recipe from-file --name recipe --execution-type post-ambari-start --file ${RECIPE_FILE} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == POST_AMBARI_START ]]
 }

@test "Check recipe create from file post-cluster-install" {
  OUTPUT=$(create-recipe from-file --name recipe --execution-type post-cluster-install --file ${RECIPE_FILE} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == POST_CLUSTER_INSTALL ]]
 }

@test "Check recipe create from file pre-termination" {
  OUTPUT=$(create-recipe from-file --name recipe --execution-type pre-termination --file ${RECIPE_FILE} 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["content","description","name","recipeType"]') == true ]]
  [[ $(echo "${OUTPUT}" | jq -r ."recipeType") == PRE_TERMINATION ]]
 }

@test "Check recipes are listed" {
  for OUTPUT in $(list-recipes | jq ' .[] | [to_entries[].key] == ["Name","Description","ExecutionType"]');
  do
    [[ "$OUTPUT" == "true" ]]
  done
 }

@test "Check recipe is described" {
  OUTPUT=$( describe-recipe --name testrecipe | jq ' . | [to_entries[].key] == ["Name","Description","ExecutionType"]')

  [[ "$OUTPUT" == "true" ]]
 }

@test "Check recipe delete" {
  OUTPUT=$(delete-recipe --name testrecipe 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"recipe deleted, name: test"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}