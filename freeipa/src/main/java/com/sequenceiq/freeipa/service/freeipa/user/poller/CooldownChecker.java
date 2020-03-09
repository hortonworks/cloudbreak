package com.sequenceiq.freeipa.service.freeipa.user.poller;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;

@Component
class CooldownChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncPoller.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public boolean isCooldownExpired(UserSyncStatus userSyncStatus, Instant cooldownThresholdTime) {
        boolean cool = userSyncStatus == null ||
                userSyncStatus.getLastRequestedFullSync() == null ||
                userSyncStatus.getLastRequestedFullSync().getStartTime() == null ||
                Instant.ofEpochMilli(userSyncStatus.getLastRequestedFullSync().getStartTime()).isBefore(cooldownThresholdTime);
        if (LOGGER.isDebugEnabled()) {
            Stack stack = userSyncStatus.getStack();
            LOGGER.debug("Synchronization to Environment {} in Account {} {} been run since {}", stack.getEnvironmentCrn(),
                    stack.getAccountId(), cool ? "has not" : "has", DATE_TIME_FORMATTER.format(cooldownThresholdTime));
        }
        return cool;
    }
}
