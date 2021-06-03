package com.sequenceiq.cloudbreak.cloud.aws.common.poller;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public abstract class CancellableAttemptMaker<V> implements AttemptMaker<V> {

    private final Long stackId;

    protected CancellableAttemptMaker(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public AttemptResult<V> process() throws Exception {
        if (cancelled()) {
            return AttemptResults.breakFor(new CancellationException("The flow is cancelled"));
        }
        return doProcess();
    }

    protected abstract AttemptResult<V> doProcess();

    private boolean cancelled() {
        PollGroup pollGroup = InMemoryStateStore.getStack(stackId);
        return CANCELLED.equals(pollGroup);
    }
}
