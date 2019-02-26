package com.sequenceiq.cloudbreak.cloud.task;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class AbstractPollTask<T> implements PollTask<T> {

    private final AuthenticatedContext authenticatedContext;

    private final boolean cancellable;

    private final Map<String, String> mdcContextMap;

    protected AbstractPollTask(AuthenticatedContext authenticatedContext) {
        this(authenticatedContext, true);
    }

    protected AbstractPollTask(AuthenticatedContext authenticatedContext, boolean cancellable) {
        this.authenticatedContext = authenticatedContext;
        this.cancellable = cancellable;
        mdcContextMap = MDCBuilder.getMdcContextMap();
    }

    @Override
    public final T call() {
        MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        return doCall();
    }

    protected abstract T doCall();

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
        return CANCELLED.equals(pollGroup);
    }
}
