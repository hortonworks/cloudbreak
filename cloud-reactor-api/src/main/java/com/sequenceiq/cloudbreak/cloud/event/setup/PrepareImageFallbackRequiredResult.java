package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PrepareImageFallbackRequiredResult extends PrepareImageResult {

    @JsonCreator
    public PrepareImageFallbackRequiredResult(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }
}
