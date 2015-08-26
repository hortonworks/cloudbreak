package com.sequenceiq.cloudbreak.cloud.task;

import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.Status;

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

    @Override
    public boolean cancelled() {
        Status stackStatus = InMemoryStateStore.get(getCloudContext().getStackId());
        return stackStatus != null && (DELETE_COMPLETED.equals(stackStatus) || DELETE_IN_PROGRESS.equals(stackStatus));
    }
}
