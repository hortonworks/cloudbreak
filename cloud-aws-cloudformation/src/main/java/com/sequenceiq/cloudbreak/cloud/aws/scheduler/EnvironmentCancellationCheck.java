package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

public class EnvironmentCancellationCheck implements CancellationCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCancellationCheck.class);

    private final Long environmentId;

    private final String environmentName;

    public EnvironmentCancellationCheck(Long environmentId, String environmentName) {
        this.environmentId = environmentId;
        this.environmentName = environmentName;
    }

    @Override
    public boolean isCancelled() {
        PollGroup environmentPollGroup = InMemoryResourceStateStore.getResource("environment", environmentId);
        if (environmentPollGroup == null || environmentPollGroup.isCancelled()) {
            LOGGER.info("Cancelling the polling of environment's '{}' Network creation, because a delete operation has already been "
                    + "started on the environment", environmentName);
            return true;
        }
        return false;
    }
}
