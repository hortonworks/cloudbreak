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
        LOGGER.info("Update environment flow state in the memory by new status: {}", newStatus);
        if (newStatus.isSuccessfullyDeleted()) {
            LOGGER.debug("Delete environment from in the memory store.");
            EnvironmentInMemoryStateStore.delete(id);
        } else if (newStatus.isDeleteInProgress()) {
            LOGGER.debug("Cancel environment flow state in the memory store deletion in progress");
            EnvironmentInMemoryStateStore.put(id, PollGroup.CANCELLED);
        } else {
            LOGGER.debug("Environment flow state added to in memory store.");
            EnvironmentInMemoryStateStore.put(id, PollGroup.POLLABLE);
        }
    }
}
