T_composeGetOldImages() {
    local all_images="sequenceiq/sultans-bin:0.5.1 sequenceiq/sultans-bin:0.5.2"
    local keep_images="sequenceiq/sultans-bin:0.5.2"
    result=$(compose-get-old-images <(echo $all_images) <(echo $keep_images))
    [[ "$result" == "sequenceiq/sultans-bin:0.5.1" ]]
}

T_composeGetOldImages_multipleDeletion() {
    local all_images="sequenceiq/sultans-bin:0.5.1 sequenceiq/sultans-bin:0.5.2 sequenceiq/periscope:0.5.2"
    local keep_images="sequenceiq/sultans-bin:0.5.2 sequenceiq/periscope:0.5.3"
    result=$(compose-get-old-images <(echo $all_images) <(echo $keep_images))
    [[ "$result" == "sequenceiq/sultans-bin:0.5.1 sequenceiq/periscope:0.5.2" ]]
}

T_composeGetOldImages_skipDifferentImages() {
   local all_images="sequenceiq/sultans-bin:0.5.1 sequenceiq/sultans-bin:0.5.2 sequenceiq/different"
   local keep_images="sequenceiq/sultans-bin:0.5.2"
   result=$(compose-get-old-images <(echo $all_images) <(echo $keep_images))
   [[ "$result" == "sequenceiq/sultans-bin:0.5.1" ]]
}

T_composeGetOldImages_noOldImages() {
   local all_images="sequenceiq/sultans-bin:0.5.2"
   local keep_images="sequenceiq/sultans-bin:0.5.2"
   result=$(compose-get-old-images <(echo $all_images) <(echo $keep_images))
   [[ -z "$result" ]]
}
