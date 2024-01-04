package com.sequenceiq.cloudbreak.cloud.task;


import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public abstract class PollBooleanStateTask extends AbstractPollTask<Boolean> {

    @Inject
    protected PollBooleanStateTask(AuthenticatedContext authenticatedContext, boolean cancellable) {
        super(authenticatedContext, cancellable);
    }

    @Override
    public boolean completed(Boolean aBoolean) {
        return aBoolean;
    }
}
