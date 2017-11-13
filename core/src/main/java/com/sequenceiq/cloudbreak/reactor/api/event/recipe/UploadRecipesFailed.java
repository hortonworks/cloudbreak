package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UploadRecipesFailed extends StackFailureEvent {
    public UploadRecipesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
