load ../commands

 RECIPE_URL=https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh
 RECIPE_FILE="e2e/recipe.sh"
 RECIPE_NAME=cli-recipe


 @test "Check recipe create from url pre-ambari-start" {
    run create-recipe from-url --name $RECIPE_NAME --execution-type pre-ambari-start --url ${RECIPE_URL}
    echo $output
    [ $status = 0 ]
 }

 @test "Check recipes are listed" {
    CHECK_RESULT=$( list-recipes )
    [ $(echo $CHECK_RESULT |  jq ' .[0] | [to_entries[].key] == ["Name","Description","ExecutionType"]' ) == true ]
 }

  @test "Check recipe is described" {
    CHECK_RESULT=$( describe-recipe --name $RECIPE_NAME )
    [ $(echo $CHECK_RESULT |  jq ' . | [to_entries[].key] == ["Name","Description","ExecutionType"]' ) == true ]
  }

  @test "Check recipe delete" {
    run delete-recipe --name $RECIPE_NAME
    echo $output
    [ $status = 0 ]
  }


