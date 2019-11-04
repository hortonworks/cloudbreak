package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UsersyncPoller {
    @VisibleForTesting
    static final String INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersyncPoller.class);

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private StackService stackService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private UserService userService;

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Value("${freeipa.syncoperation.poller.enabled:true}")
    private boolean enabled;

    @Inject
    private EntitlementService entitlementService;

    @Scheduled(fixedDelayString = "${freeipa.syncoperation.poller.fixed-delay-millis:60000}",
            initialDelayString = "${freeipa.syncoperation.poller.initial-delay-millis:300000}")
    public void pollUms() {
        try {
            if (enabled) {
                syncFreeIpaStacks();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to automatically sync users to FreeIPA stacks", e);
        }
    }

    @VisibleForTesting
    void syncFreeIpaStacks() {
        threadBasedUserCrnProvider.setUserCrn(INTERNAL_ACTOR_CRN);
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        LOGGER.debug("Setting request id = {} for this poll", requestId);
        MDCBuilder.addRequestId(requestId.get());
        try {
            LOGGER.debug("Attempting to sync users to FreeIPA stacks");
            List<Stack> stackList = stackService.findAllRunning();
            LOGGER.debug("Found {} active stacks", stackList.size());

            stackList.stream()
                    .collect(Collectors.groupingBy(Stack::getAccountId))
                    .entrySet().stream()
                    .filter(stringListEntry -> {
                        String accountId = stringListEntry.getKey();
                        boolean entitled = entitlementService.automaticUsersyncPollerEnabled(INTERNAL_ACTOR_CRN, accountId);
                        if (!entitled) {
                            LOGGER.debug("Usersync polling is not entitled in accout {}. skipping", accountId);
                        }
                        return entitled;
                    })
                    .forEach(stringListEntry -> {
                        String accountId = stringListEntry.getKey();
                        LOGGER.debug("Usersync polling is entitled in account {}", accountId);
                        UmsEventGenerationIds currentGeneration =
                                umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId);
                        stringListEntry.getValue().stream()
                                .forEach(stack -> {
                                    if (isStale(stack, currentGeneration)) {
                                        LOGGER.debug("Environment {} in Account {} is stale.", stack.getEnvironmentCrn(), stack.getAccountId());
                                        SyncOperationStatus status = userService.synchronizeUsers(stack.getAccountId(), INTERNAL_ACTOR_CRN,
                                                Set.of(stack.getEnvironmentCrn()), Set.of(), Set.of());
                                        LOGGER.debug("Sync request resulted in operation {}", status);
                                    } else {
                                        LOGGER.debug("Environment {} in Account {} is up-to-date.", stack.getEnvironmentCrn(), stack.getAccountId());
                                    }
                                });
                    });
        } finally {
            threadBasedUserCrnProvider.removeUserCrn();
            MDCBuilder.cleanupMdc();
        }
    }

    @VisibleForTesting
    boolean isStale(Stack stack, UmsEventGenerationIds currentGeneration) {
        try {
            UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
            return !currentGeneration.equals(userSyncStatus.getUmsEventGenerationIds().get(UmsEventGenerationIds.class));
        } catch (Exception e) {
            LOGGER.warn("Unable to calculate staleness due to exception.", e);
            return false;
        }
    }
}
