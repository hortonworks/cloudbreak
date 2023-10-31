package com.sequenceiq.freeipa.service.freeipa.user;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class EnvironmentUserSyncStateCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUserSyncStateCalculator.class);

    @Inject
    private StackService stackService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Inject
    private EventGenerationIdsChecker eventGenerationIdsChecker;

    public EnvironmentUserSyncState calculateEnvironmentUserSyncState(String accountId, Crn environmentCrn) {
        checkArgument(accountId.equals(environmentCrn.getAccountId()), "environmentCrn does not match account id");
        String envCrnString = environmentCrn.toString();

        Stack stack = stackService.getByEnvironmentCrnAndAccountId(envCrnString, accountId);
        MDCBuilder.buildMdcContext(stack);

        Optional<UserSyncStatus> userSyncStatus = userSyncStatusService.findByStack(stack);
        return internalCalculateEnvironmentUserSyncState(stack, envCrnString, userSyncStatus);
    }

    private EnvironmentUserSyncState internalCalculateEnvironmentUserSyncState(Stack stack, String envCrnString, Optional<UserSyncStatus> userSyncStatus) {
        EnvironmentUserSyncState environmentUserSyncState = new EnvironmentUserSyncState();
        if (userSyncStatus.isEmpty() || userSyncStatus.get().getLastStartedFullSync() == null) {
            environmentUserSyncState.setState(UserSyncState.STALE);
        } else {
            environmentUserSyncState.setLastUserSyncOperationId(userSyncStatus.get().getLastStartedFullSync().getOperationId());
            environmentUserSyncState.setState(calculateUserSyncState(stack, envCrnString, userSyncStatus.get()));
        }
        LOGGER.debug("Calculated usr sync state: [{}]", environmentUserSyncState);
        return environmentUserSyncState;
    }

    private UserSyncState calculateUserSyncState(Stack stack, String envCrnString, UserSyncStatus userSyncStatus) {
        Operation lastSync = userSyncStatus.getLastStartedFullSync();
        // REQUESTED or REJECTED operations will never be saved as part of the UserSyncStatus
        return switch (lastSync.getStatus()) {
            case RUNNING -> UserSyncState.SYNC_IN_PROGRESS;
            case COMPLETED -> calculateStateForCompletedOperation(stack, envCrnString, userSyncStatus);
            case REQUESTED, REJECTED -> throw createExceptionForUnexpectedOperationStatus(envCrnString, userSyncStatus);
            case TIMEDOUT -> {
                LOGGER.warn("UserSyncStatus.lastStartedFullSync '{}' is timed out for environment '{}'", lastSync.getOperationId(), envCrnString);
                yield UserSyncState.SYNC_FAILED;
            }
            case FAILED -> UserSyncState.SYNC_FAILED;
            default -> UserSyncState.STALE;
        };
    }

    private UserSyncState calculateStateForCompletedOperation(Stack stack, String envCrnString, UserSyncStatus userSyncStatus) {
        Operation lastSync = userSyncStatus.getLastStartedFullSync();
        if (environmentUserSyncSucceeded(lastSync, envCrnString)) {
            UmsEventGenerationIds currentEventGenerationIds = umsEventGenerationIdsProvider.getEventGenerationIds(stack.getAccountId());
            if (eventGenerationIdsChecker.isInSync(userSyncStatus, currentEventGenerationIds, stack)) {
                return UserSyncState.UP_TO_DATE;
            } else {
                return UserSyncState.STALE;
            }
        } else {
            return UserSyncState.SYNC_FAILED;
        }
    }

    private IllegalStateException createExceptionForUnexpectedOperationStatus(String envCrnString, UserSyncStatus userSyncStatus) {
        Operation lastSync = userSyncStatus.getLastStartedFullSync();
        LOGGER.error("UserSyncStatus.lastStartedFullSync '{}' in unexpected state {} for environment '{}'",
                lastSync, lastSync.getStatus(), envCrnString);
        return new IllegalStateException(
                String.format("Last sync operation [%s] for environment '%s' is in unexpected state %s",
                        lastSync.getOperationId(), envCrnString, lastSync.getStatus()));
    }

    private boolean environmentUserSyncSucceeded(Operation syncOperation, String environmentCrn) {
        return syncOperation.getSuccessList().stream()
                .anyMatch(details -> environmentCrn.equals(details.getEnvironment()));
    }
}
