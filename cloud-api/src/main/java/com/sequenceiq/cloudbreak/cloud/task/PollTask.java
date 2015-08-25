package com.sequenceiq.cloudbreak.cloud.task;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public abstract class PollTask<T> implements FetchTask<T>, CheckResult<T> {

    private final CloudConnector connector;
    private final AuthenticatedContext authenticatedContext;

    public PollTask(CloudConnector connector, AuthenticatedContext authenticatedContext) {
        this.connector = connector;
        this.authenticatedContext = authenticatedContext;
    }

    @Override
    public CloudContext getCloudContext() {
        return authenticatedContext.getCloudContext();
    }

    protected CloudConnector getConnector() {
        return connector;
    }

    protected AuthenticatedContext getAuthenticatedContext() {
        return authenticatedContext;
    }
}
