package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.EventGenerationIdsChecker;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
@ConditionalOnProperty(
        value = "freeipa.usersync.poller.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class UserSyncPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncPoller.class);

    @VisibleForTesting
    @Value("${freeipa.usersync.poller.cooldown-duration}")
    Duration cooldown;

    @Inject
    private StackService stackService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private UserSyncService userSyncService;

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Inject
    private UserSyncPollerEntitlementChecker userSyncPollerEntitlementChecker;

    @Inject
    private EventGenerationIdsChecker eventGenerationIdsChecker;

    @Inject
    private CooldownChecker cooldownChecker;

    @Scheduled(fixedDelayString = "${freeipa.usersync.poller.fixed-delay-millis}",
            initialDelayString = "${freeipa.usersync.poller.initial-delay-millis}")
    public void automaticUserSyncTask() {
        try {
            LOGGER.debug("Polling for automatic user sync");
            syncAllFreeIpaStacks();
        } catch (Exception e) {
            LOGGER.error("Failed to automatically sync users to FreeIPA stacks", e);
        }
    }

    @VisibleForTesting
    void syncAllFreeIpaStacks() {
        try {
            Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
            MDCBuilder.addRequestId(requestId.get());
            LOGGER.debug("Setting request id = {} for this poll", requestId);

            ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> {
                LOGGER.debug("Attempting to sync users to FreeIPA stacks");
                List<Stack> stackList = stackService.findAllWithStatuses(Status.AVAILABLE_STATUSES);
                LOGGER.debug("Found {} active stacks", stackList.size());

                stackList.stream()
                        .collect(Collectors.groupingBy(Stack::getAccountId))
                        .entrySet().stream()
                        .forEach(stringListEntry -> {
                            String accountId = stringListEntry.getKey();
                            if (userSyncPollerEntitlementChecker.isAccountEntitled(accountId)) {
                                LOGGER.debug("Automatic usersync polling is entitled in account {}", accountId);
                                syncFreeIpaStacksInAccount(requestId, accountId, stringListEntry.getValue());
                            } else {
                                LOGGER.debug("Automatic usersync polling is not entitled in account {}.", accountId);
                            }
                        });
            });
        } finally {
            MDCBuilder.cleanupMdc();
        }
    }

    private void syncFreeIpaStacksInAccount(Optional<String> requestId, String accountId, List<Stack> stacks) {
        Instant cooldownThresholdTime = Instant.now().minus(cooldown);
        UmsEventGenerationIds currentGeneration =
                umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId);

        stacks.forEach(stack -> {
            UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
            if (!eventGenerationIdsChecker.isInSync(userSyncStatus, currentGeneration) &&
                    cooldownChecker.isCooldownExpired(userSyncStatus, cooldownThresholdTime)) {
                LOGGER.debug("Environment {} in Account {} is not in sync.",
                        stack.getEnvironmentCrn(), stack.getAccountId());
                Operation operation = userSyncService.synchronizeUsers(stack.getAccountId(), INTERNAL_ACTOR_CRN,
                        Set.of(stack.getEnvironmentCrn()), Set.of(), Set.of(), false);
                LOGGER.debug("User Sync request resulted in operation {}", operation);
            } else {
                LOGGER.debug("Environment {} in Account {} is in sync or has been synchronized recently.", stack.getEnvironmentCrn(), stack.getAccountId());
            }
        });
    }
}
