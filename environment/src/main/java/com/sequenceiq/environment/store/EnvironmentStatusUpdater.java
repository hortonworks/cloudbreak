package com.sequenceiq.environment.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.EnvironmentStatus;

public class EnvironmentStatusUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStatusUpdater.class);

    private EnvironmentStatusUpdater() {
    }

    public static void update(Long id, EnvironmentStatus newStatus) {
        LOGGER.info("Update environment flow state in the memory state by new status: {}", newStatus);
        if (newStatus.isSuccessfullyDeleted()) {
            EnvironmentInMemoryStateStore.delete(id);
        } else if (newStatus.isDeleteInProgress()) {
            EnvironmentInMemoryStateStore.put(id, PollGroup.CANCELLED);
        } else {
            EnvironmentInMemoryStateStore.put(id, PollGroup.POLLABLE);
        }
    }
}
