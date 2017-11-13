package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UploadRecipesSuccess extends StackEvent {
    public UploadRecipesSuccess(Long stackId) {
        super(stackId);
    }
}
