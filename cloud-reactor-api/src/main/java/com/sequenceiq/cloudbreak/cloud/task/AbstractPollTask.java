package com.sequenceiq.cloudbreak.cloud.task;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public abstract class AbstractPollTask<T> implements PollTask<T> {

    private final AuthenticatedContext authenticatedContext;

    private final boolean cancellable;

    protected AbstractPollTask(AuthenticatedContext authenticatedContext) {
        this(authenticatedContext, true);
    }

    protected AbstractPollTask(AuthenticatedContext authenticatedContext, boolean cancellable) {
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
        PollGroup pollGroup = InMemoryStateStore.getStack(authenticatedContext.getCloudContext().getId());
        return pollGroup != null && CANCELLED.equals(pollGroup);
    }
}
