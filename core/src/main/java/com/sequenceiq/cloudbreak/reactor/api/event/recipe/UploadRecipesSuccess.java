package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UploadRecipesSuccess extends StackEvent {
    @JsonCreator
    public UploadRecipesSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
