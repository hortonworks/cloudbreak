load ../commands

RECIPE_URL=https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh
RECIPE_FILE="e2e/recipe.sh"

@test "Check recipe create from url pre-ambari-start" {
   run create-recipe from-url --name recipe --execution-type pre-ambari-start --url ${RECIPE_URL}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from url post-ambari-start" {
   run create-recipe from-url --name recipe --execution-type post-ambari-start --url ${RECIPE_URL}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from url post-cluster-install" {
   run create-recipe from-url --name recipe --execution-type post-cluster-install --url ${RECIPE_URL}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from url pre-termination" {
   run create-recipe from-url --name recipe --execution-type pre-termination --url ${RECIPE_URL}
   echo $output
   [ $status = 0 ]
 }

@test "Check recipe create from file pre-ambari-start" {
   run create-recipe from-file --name recipe --execution-type pre-ambari-start --file ${RECIPE_FILE}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from file post-ambari-start" {
   run create-recipe from-file --name recipe --execution-type post-ambari-start --file ${RECIPE_FILE}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from file post-cluster-install" {
   run create-recipe from-file --name recipe --execution-type post-cluster-install --file ${RECIPE_FILE}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipe create from file pre-termination" {
   run create-recipe from-file --name recipe --execution-type pre-termination --url ${RECIPE_URL}
   echo $output
   [ $status = 0 ]
 }

 @test "Check recipes are listed" {
   CHECK_RESULT=$( list-recipes )
   [ $(echo $CHECK_RESULT |  jq ' .[] | [to_entries[].key] == ["Name","Description","ExecutionType"]' ) == true ]
 }

 @test "Check recipe is described" {
   CHECK_RESULT=$( describe-recipe --name testrecipe )
   [ $(echo $CHECK_RESULT |  jq ' . | [to_entries[].key] == ["Name","Description","ExecutionType"]' ) == true ]
 }

 @test "Check recipe delete" {
   CHECK_RESULT=$( delete-blueprint --name testrecipe )
   echo $CHECK_RESULT >&2
}