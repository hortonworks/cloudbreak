package com.sequenceiq.cloudbreak.cloud.task;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public abstract class PollTask<T> implements FetchTask<T>, CheckResult<T> {

    private final AuthenticatedContext authenticatedContext;

    public PollTask(AuthenticatedContext authenticatedContext) {
        this.authenticatedContext = authenticatedContext;
    }

    @Override
    public CloudContext getCloudContext() {
        return authenticatedContext.getCloudContext();
    }

    protected AuthenticatedContext getAuthenticatedContext() {
        return authenticatedContext;
    }
}
