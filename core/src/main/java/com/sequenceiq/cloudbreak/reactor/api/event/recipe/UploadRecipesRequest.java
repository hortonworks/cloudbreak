package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UploadRecipesRequest extends StackEvent {

    public UploadRecipesRequest(Long stackId) {
        super(stackId);
    }
}
