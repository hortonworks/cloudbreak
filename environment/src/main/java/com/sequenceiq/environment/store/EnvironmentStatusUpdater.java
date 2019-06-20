package com.sequenceiq.environment.store;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.EnvironmentStatus;

public class EnvironmentStatusUpdater {

    private EnvironmentStatusUpdater() {
    }

    public static void update(Long id, EnvironmentStatus newStatus) {
        if (newStatus.isSuccessfullyDeleted()) {
            EnvironmentInMemoryStateStore.delete(id);
        } else if (newStatus.isDeleteInProgress()) {
            EnvironmentInMemoryStateStore.put(id, PollGroup.CANCELLED);
        } else {
            EnvironmentInMemoryStateStore.put(id, PollGroup.POLLABLE);
        }
    }
}
