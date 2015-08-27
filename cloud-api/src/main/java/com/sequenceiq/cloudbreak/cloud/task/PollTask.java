package com.sequenceiq.cloudbreak.cloud.task;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

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
        PollGroup pollGroup = InMemoryStateStore.get(getCloudContext().getStackId());
        return pollGroup != null && CANCELLED.equals(pollGroup);
    }
}
