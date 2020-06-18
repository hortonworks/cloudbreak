package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public class StackCancellationCheck implements CancellationCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackCancellationCheck.class);

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
