load commands

 @test "Check recipes are listed" {
   CHECK_RESULT=$( region-list --credential testcred )
   [ $(echo $CHECK_RESULT |  jq ' .[] | [to_entries[].key] == ["Name","Description"]' ) == true ]
 }