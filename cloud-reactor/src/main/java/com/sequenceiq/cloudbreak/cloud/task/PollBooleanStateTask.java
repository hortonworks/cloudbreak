package com.sequenceiq.cloudbreak.cloud.task;


import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public abstract class PollBooleanStateTask extends AbstractPollTask<Boolean> {

    @Inject
    public PollBooleanStateTask(AuthenticatedContext authenticatedContext, boolean cancellable) {
        super(authenticatedContext, cancellable);
    }

    @Override
    public boolean completed(Boolean aBoolean) {
        return aBoolean;
    }
}
