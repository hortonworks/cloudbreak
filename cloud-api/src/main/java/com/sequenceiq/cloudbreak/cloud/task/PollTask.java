package com.sequenceiq.cloudbreak.cloud.task;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public abstract class PollTask<T> implements FetchTask<T>, Check<T> {

    private final AuthenticatedContext authenticatedContext;

    private final boolean cancellable;

    public PollTask(AuthenticatedContext authenticatedContext) {
        this(authenticatedContext, true);
    }

    public PollTask(AuthenticatedContext authenticatedContext, boolean cancellable) {
        this.authenticatedContext = authenticatedContext;
        this.cancellable = cancellable;
    }

    @Override
    public AuthenticatedContext getAuthenticatedContext() {
        return authenticatedContext;
    }

    @Override
    public boolean cancelled() {
        if (!cancellable) {
            return false;
        }
        PollGroup pollGroup = InMemoryStateStore.get(getAuthenticatedContext().getCloudContext().getStackId());
        return pollGroup != null && CANCELLED.equals(pollGroup);
    }
}
