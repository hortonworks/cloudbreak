package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.FULL_USER_SYNC;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.PARTIAL_USER_SYNC;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_FULL_UMS_STATE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_PARTIAL_UMS_STATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsUsersStateProviderDispatcher;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class UserSyncForEnvService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncForEnvService.class);

    @Value("#{${freeipa.operation.cleanup.timeout-millis} * 0.9 }")
    private Long operationTimeout;

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Inject
    private UserSyncForStackService userSyncForStackService;

    @Inject
    private UmsUsersStateProviderDispatcher umsUsersStateProviderDispatcher;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_INTERNAL_TASK_EXECUTOR)
    private ExecutorService asyncTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private UmsVirtualGroupCreateService umsVirtualGroupCreateService;

    @Inject
    private EntitlementService entitlementService;

    public void synchronizeUsers(String operationId, String accountId, List<StackUserSyncView> stacks, UserSyncRequestFilter userSyncFilter,
            UserSyncOptions options, long startTime) {
        operationService.tryWithOperationCleanup(operationId, accountId, () -> {
            Set<String> environmentCrns = stacks.stream().map(StackUserSyncView::environmentCrn).collect(Collectors.toSet());
            UserSyncLogEvent logUserSyncEvent = options.isFullSync() ? FULL_USER_SYNC : PARTIAL_USER_SYNC;
            LOGGER.info("Starting {} for environments {} with operationId {} ...", logUserSyncEvent, environmentCrns, operationId);

            if (options.isFullSync()) {
                umsVirtualGroupCreateService.createVirtualGroups(accountId, stacks);
            }

            Map<String, Future<SyncStatusDetail>> statusFutures =
                    startAsyncSyncsForStacks(operationId, accountId, stacks, userSyncFilter, options, environmentCrns);

            List<SuccessDetails> success = new ArrayList<>();
            List<FailureDetails> failure = new ArrayList<>();

            statusFutures.forEach((envCrn, statusFuture) -> {
                try {
                    SyncStatusDetail statusDetail = waitForSyncStatusDetailResult(startTime, statusFuture, accountId);
                    switch (statusDetail.getStatus()) {
                        case COMPLETED:
                            success.add(new SuccessDetails(envCrn));
                            break;
                        case FAILED:
                            failure.add(createFailureDetails(envCrn, statusDetail.getDetails(), statusDetail.getWarnings()));
                            break;
                        default:
                            failure.add(createFailureDetails(envCrn, "Unexpected status: " + statusDetail.getStatus(), statusDetail.getWarnings()));
                            break;
                    }
                } catch (TimeoutException e) {
                    LOGGER.warn("Sync timed out for env: {}", envCrn, e);
                    statusFuture.cancel(true);
                    failure.add(new FailureDetails(envCrn, "Timed out"));
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Sync is interrupted for env: {}", envCrn, e);
                    failure.add(new FailureDetails(envCrn, e.getLocalizedMessage()));
                }
            });
            operationService.completeOperation(accountId, operationId, success, failure);
            LOGGER.info("Finished {} for environments {} with operationId {}.", logUserSyncEvent, environmentCrns, operationId);
        });
    }

    private SyncStatusDetail waitForSyncStatusDetailResult(long startTime, Future<SyncStatusDetail> statusFuture, String accountId)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (entitlementService.isUserSyncThreadTimeoutEnabled(accountId)) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long timeout = operationTimeout - elapsedTime;
            LOGGER.debug("Get SyncStatusDetail with {}ms timeout", timeout);
            return statusFuture.get(timeout < 0 ? 0 : timeout, TimeUnit.MILLISECONDS);
        } else {
            LOGGER.debug("Get SyncStatusDetail without timeout");
            return statusFuture.get();
        }
    }

    private Map<String, Future<SyncStatusDetail>> startAsyncSyncsForStacks(String operationId, String accountId, List<StackUserSyncView> stacks,
            UserSyncRequestFilter userSyncFilter, UserSyncOptions options, Set<String> environmentCrns) {
        if (userSyncFilter.getDeletedWorkloadUser().isEmpty()) {
            UserSyncLogEvent logRetrieveUmsEvent = options.isFullSync() ? RETRIEVE_FULL_UMS_STATE : RETRIEVE_PARTIAL_UMS_STATE;
            LOGGER.debug("Starting {} for environments {} ...", logRetrieveUmsEvent, environmentCrns);
            Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProviderDispatcher
                    .getEnvToUmsUsersStateMap(accountId, environmentCrns, userSyncFilter.getUserCrnFilter(),
                            userSyncFilter.getMachineUserCrnFilter(), options);
            LOGGER.debug("Finished {}.", logRetrieveUmsEvent);
            UmsEventGenerationIds umsEventGenerationIds = options.isFullSync() ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId) : null;
            return stacks.stream()
                    .collect(Collectors.toMap(StackUserSyncView::environmentCrn,
                            stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.environmentCrn()), umsEventGenerationIds, options,
                                    operationId, accountId)));
        } else {
            String deletedWorkloadUser = userSyncFilter.getDeletedWorkloadUser().get();
            return stacks.stream()
                    .collect(Collectors.toMap(StackUserSyncView::environmentCrn, stack -> asyncSynchronizeStackForDeleteUser(stack, deletedWorkloadUser)));
        }
    }

    private FailureDetails createFailureDetails(String envCrn, String details, Multimap<String, String> warnings) {
        FailureDetails failureDetails = new FailureDetails(envCrn, details);
        Map<String, String> additionalDetails = failureDetails.getAdditionalDetails();
        warnings.asMap().forEach((key, value) -> additionalDetails.put(key, String.join(", ", value)));
        return failureDetails;
    }

    private Future<SyncStatusDetail> asyncSynchronizeStack(StackUserSyncView stack, UmsUsersState umsUsersState, UmsEventGenerationIds umsEventGenerationIds,
            UserSyncOptions options, String operationId, String accountId) {
        return asyncTaskExecutor.submit(() -> {
            SyncStatusDetail statusDetail = userSyncForStackService.synchronizeStack(stack, umsUsersState, options, operationId);
            if (options.isFullSync() && statusDetail.getStatus() == SynchronizationStatus.COMPLETED) {
                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack.id());
                userSyncStatus.setUmsEventGenerationIds(new Json(umsEventGenerationIds));
                userSyncStatus.setLastSuccessfulFullSync(operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
                userSyncStatusService.save(userSyncStatus);
            }
            return statusDetail;
        });
    }

    private Future<SyncStatusDetail> asyncSynchronizeStackForDeleteUser(StackUserSyncView stack, String deletedWorkloadUser) {
        return asyncTaskExecutor.submit(() -> userSyncForStackService.synchronizeStackForDeleteUser(stack, deletedWorkloadUser));
    }
}
