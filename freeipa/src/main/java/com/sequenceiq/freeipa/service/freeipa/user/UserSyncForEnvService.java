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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
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

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Inject
    private UserSyncForStackService userSyncForStackService;

    @Inject
    private UmsUsersStateProviderDispatcher umsUsersStateProviderDispatcher;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_TASK_EXECUTOR)
    private ExecutorService asyncTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private UmsVirtualGroupCreateService umsVirtualGroupCreateService;

    public void synchronizeUsers(String operationId, String accountId, List<Stack> stacks, UserSyncRequestFilter userSyncFilter,
            UserSyncOptions options) {
        operationService.tryWithOperationCleanup(operationId, accountId, () -> {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());
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
                    SyncStatusDetail statusDetail = statusFuture.get();
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
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Sync is interrupted for env: {}", envCrn, e);
                    failure.add(new FailureDetails(envCrn, e.getLocalizedMessage()));
                }
            });
            operationService.completeOperation(accountId, operationId, success, failure);
            LOGGER.info("Finished {} for environments {} with operationId {}.", logUserSyncEvent, environmentCrns, operationId);
        });
    }

    private Map<String, Future<SyncStatusDetail>> startAsyncSyncsForStacks(String operationId, String accountId, List<Stack> stacks,
            UserSyncRequestFilter userSyncFilter, UserSyncOptions options, Set<String> environmentCrns) {
        if (userSyncFilter.getDeletedWorkloadUser().isEmpty()) {
            UserSyncLogEvent logRetrieveUmsEvent = options.isFullSync() ? RETRIEVE_FULL_UMS_STATE : RETRIEVE_PARTIAL_UMS_STATE;
            LOGGER.debug("Starting {} for environments {} ...", logRetrieveUmsEvent, environmentCrns);
            Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProviderDispatcher
                    .getEnvToUmsUsersStateMap(accountId, environmentCrns, userSyncFilter.getUserCrnFilter(),
                            userSyncFilter.getMachineUserCrnFilter(), MDCUtils.getRequestId());
            LOGGER.debug("Finished {}.", logRetrieveUmsEvent);
            UmsEventGenerationIds umsEventGenerationIds = options.isFullSync() ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId, MDCUtils.getRequestId()) : null;
            return stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                            stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), umsEventGenerationIds, options,
                                    operationId, accountId)));
        } else {
            String deletedWorkloadUser = userSyncFilter.getDeletedWorkloadUser().get();
            return stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn, stack -> asyncSynchronizeStackForDeleteUser(stack, deletedWorkloadUser)));
        }
    }

    private FailureDetails createFailureDetails(String envCrn, String details, Multimap<String, String> warnings) {
        FailureDetails failureDetails = new FailureDetails(envCrn, details);
        Map<String, String> additionalDetails = failureDetails.getAdditionalDetails();
        warnings.asMap().forEach((key, value) -> additionalDetails.put(key, String.join(", ", value)));
        return failureDetails;
    }

    private Future<SyncStatusDetail> asyncSynchronizeStack(Stack stack, UmsUsersState umsUsersState, UmsEventGenerationIds umsEventGenerationIds,
            UserSyncOptions options, String operationId, String accountId) {
        return asyncTaskExecutor.submit(() -> {
            SyncStatusDetail statusDetail = userSyncForStackService.synchronizeStack(stack, umsUsersState, options);
            if (options.isFullSync() && statusDetail.getStatus() == SynchronizationStatus.COMPLETED) {
                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                userSyncStatus.setUmsEventGenerationIds(new Json(umsEventGenerationIds));
                userSyncStatus.setLastSuccessfulFullSync(operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
                userSyncStatusService.save(userSyncStatus);
            }
            return statusDetail;
        });
    }

    private Future<SyncStatusDetail> asyncSynchronizeStackForDeleteUser(Stack stack, String deletedWorkloadUser) {
        return asyncTaskExecutor.submit(() -> userSyncForStackService.synchronizeStackForDeleteUser(stack, deletedWorkloadUser));
    }
}
