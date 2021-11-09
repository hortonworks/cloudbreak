package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public class StackCancellationCheck implements CancellationCheck {

    private final Long stackId;

    public StackCancellationCheck(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public boolean isCancelled() {
        PollGroup pollGroup = InMemoryStateStore.getStack(stackId);
        return CANCELLED.equals(pollGroup);
    }
}
