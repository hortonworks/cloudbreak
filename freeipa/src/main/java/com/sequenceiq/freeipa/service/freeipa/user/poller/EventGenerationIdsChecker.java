package com.sequenceiq.freeipa.service.freeipa.user.poller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Component
class EventGenerationIdsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventGenerationIdsChecker.class);

    public boolean isInSync(UserSyncStatus userSyncStatus, UmsEventGenerationIds currentGeneration) {
        Stack stack = userSyncStatus.getStack();
        boolean inSync = userSyncStatus != null &&
                userSyncStatus.getUmsEventGenerationIds() != null;
        if (inSync) {
            try {
                UmsEventGenerationIds lastUmsEventGenerationIds = userSyncStatus.getUmsEventGenerationIds().get(UmsEventGenerationIds.class);
                inSync = currentGeneration.equals(lastUmsEventGenerationIds);
            } catch (IOException e) {
                LOGGER.warn("Failed to retrieve UmsEventGenerationIds for Environment {} in Account {}. Assuming not in sync",
                        stack.getEnvironmentCrn(), stack.getAccountId());
                inSync = false;
            }
        }
        LOGGER.debug("Environment {} in Account {} {} in sync", stack.getEnvironmentCrn(), stack.getAccountId(), inSync ? "is" : "is not");
        return inSync;
    }

}
