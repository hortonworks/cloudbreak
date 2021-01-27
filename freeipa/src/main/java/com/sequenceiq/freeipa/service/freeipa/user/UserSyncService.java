package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.client.FreeIpaCapabilities;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.operation.AbstractFreeipaOperation;
import com.sequenceiq.freeipa.client.operation.GroupAddMemberOperation;
import com.sequenceiq.freeipa.client.operation.GroupAddOperation;
import com.sequenceiq.freeipa.client.operation.GroupRemoveMemberOperation;
import com.sequenceiq.freeipa.client.operation.GroupRemoveOperation;
import com.sequenceiq.freeipa.client.operation.UserAddOperation;
import com.sequenceiq.freeipa.client.operation.UserRemoveOperation;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsUsersStateProviderDispatcher;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UserSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncService.class);

    private enum LogEvent {
        FULL_USER_SYNC,
        PARTIAL_USER_SYNC,
        USER_SYNC_DELETE,
        RETRIEVE_FULL_UMS_STATE,
        RETRIEVE_PARTIAL_UMS_STATE,
        RETRIEVE_FULL_IPA_STATE,
        RETRIEVE_PARTIAL_IPA_STATE,
        CALCULATE_UMS_IPA_DIFFERENCE,
        APPLY_DIFFERENCE_TO_IPA,
        SET_WORKLOAD_CREDENTIALS,
        SYNC_CLOUD_IDENTITIES,
        ADD_GROUPS,
        ADD_USERS,
        ADD_USERS_TO_GROUPS,
        REMOVE_USERS_FROM_GROUPS,
        REMOVE_USERS,
        REMOVE_GROUPS
    }

    @VisibleForTesting
    @Value("${freeipa.usersync.max-subjects-per-request}")
    int maxSubjectsPerRequest;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UmsUsersStateProviderDispatcher umsUsersStateProviderDispatcher;

    @Inject
    private FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_TASK_EXECUTOR)
    private AsyncTaskExecutor asyncTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Inject
    private UserSyncStatusService userSyncStatusService;

    @Inject
    private WorkloadCredentialService workloadCredentialService;

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private CloudIdentitySyncService cloudIdentitySyncService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UserSyncRequestValidator userSyncRequestValidator;

    @Inject
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    public Operation synchronizeUsers(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(userCrnFilter, machineUserCrnFilter, Optional.empty());
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userSyncFilter);
        return performSyncForStacks(accountId, actorCrn, userSyncFilter, stacks);
    }

    public Operation synchronizeUsersWithCustomPermissionCheck(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            UserSyncRequestFilter userSyncFilter, AuthorizationResourceAction action) {
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userSyncFilter);
        List<String> relatedEnvironmentCrns = stacks.stream().map(stack -> stack.getEnvironmentCrn()).collect(Collectors.toList());
        CustomCheckUtil.run(actorCrn, () -> commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, actorCrn, relatedEnvironmentCrns));
        return performSyncForStacks(accountId, actorCrn, userSyncFilter, stacks);
    }

    private Operation performSyncForStacks(String accountId, String actorCrn, UserSyncRequestFilter userSyncFilter, List<Stack> stacks) {
        logAffectedStacks(stacks);
        Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());
        Operation operation = operationService
                .startOperation(accountId, OperationType.USER_SYNC, environmentCrns, union(userSyncFilter.getUserCrnFilter(),
                        userSyncFilter.getMachineUserCrnFilter()));

        String operationId = operation.getOperationId();
        OperationState operationState = operation.getStatus();
        LOGGER.info("Starting operation [{}] with status [{}]", operationId, operationState);

        if (operationState == OperationState.RUNNING) {
            tryWithOperationCleanup(operationId, accountId, () ->
                    ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> {
                        boolean fullSync = userSyncFilter.isFullSync();
                        if (fullSync) {
                            stacks.forEach(stack -> {
                                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                                userSyncStatus.setLastStartedFullSync(operation);
                                userSyncStatusService.save(userSyncStatus);
                            });
                        }
                        asyncSynchronizeUsers(operation.getOperationId(), accountId, actorCrn, stacks, userSyncFilter, fullSync);
                    }));
        }

        return operation;
    }

    private void logAffectedStacks(List<Stack> stacks) {
        String stacksAffected = stacks.stream().map(stack ->
                "environment crn: [" + stack.getEnvironmentCrn() + ']'
                        + " resource crn: [" + stack.getResourceCrn() + ']'
                        + " resource name: [" + stack.getName() + ']')
                .collect(Collectors.joining("; "));
        LOGGER.info("Affected stacks: {}", stacksAffected);
    }

    private List<Stack> getStacksForSync(String accountId, String actorCrn, Set<String> environmentCrnFilter, UserSyncRequestFilter userSyncRequestFilter) {
        userSyncRequestValidator.validateParameters(accountId, actorCrn, environmentCrnFilter, userSyncRequestFilter);
        LOGGER.debug("Synchronizing users in account {} for environmentCrns {}, user sync filter {}", accountId, environmentCrnFilter, userSyncRequestFilter);

        List<Stack> stacks = stackService.getMultipleByEnvironmentCrnOrChildEnvironmantCrnAndAccountId(environmentCrnFilter, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException(String.format("No matching FreeIPA stacks found for account %s with environment crn filter %s",
                    accountId, environmentCrnFilter));
        }
        LOGGER.debug("Found {} stacks", stacks.size());
        return stacks;
    }

    @VisibleForTesting
    void asyncSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks, UserSyncRequestFilter userSyncFilter,
            boolean fullSync) {
        try {
            MDCBuilder.addOperationId(operationId);
            asyncTaskExecutor.submit(() -> internalSynchronizeUsers(operationId, accountId, actorCrn, stacks, userSyncFilter, fullSync));
        } finally {
            MDCBuilder.removeOperationId();
        }

    }

    private void tryWithOperationCleanup(String operationId, String accountId, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            try {
                LOGGER.error("Operation {} in account {} failed. Attempting to mark failure in database then re-throwing.",
                        operationId, accountId, t);
                operationService.failOperation(accountId, operationId,
                        "User sync operation failed: " + t.getLocalizedMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to mark operation {} in account {} as failed in database.", operationId, accountId, e);
            } finally {
                throw t;
            }
        }
    }

    private void internalSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks, UserSyncRequestFilter userSyncFilter,
            boolean fullSync) {
        tryWithOperationCleanup(operationId, accountId, () -> {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());

            Optional<String> requestId = MDCUtils.getRequestId();

            UmsEventGenerationIds umsEventGenerationIds = fullSync ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId) :
                    null;

            LogEvent logUserSyncEvent = fullSync ? LogEvent.FULL_USER_SYNC : LogEvent.PARTIAL_USER_SYNC;
            LOGGER.info("Starting {} for environments {} with operationId {} ...", logUserSyncEvent, environmentCrns, operationId);

            Map<String, Future<SyncStatusDetail>> statusFutures;

            if (userSyncFilter.getDeletedWorkloadUser().isEmpty()) {
                LogEvent logRetrieveUmsEvent = fullSync ? LogEvent.RETRIEVE_FULL_UMS_STATE : LogEvent.RETRIEVE_PARTIAL_UMS_STATE;
                LOGGER.debug("Starting {} for environments {} ...", logRetrieveUmsEvent, environmentCrns);
                Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProviderDispatcher
                        .getEnvToUmsUsersStateMap(accountId, actorCrn, environmentCrns, userSyncFilter.getUserCrnFilter(),
                                userSyncFilter.getMachineUserCrnFilter(), requestId);
                LOGGER.debug("Finished {}.", logRetrieveUmsEvent);
                statusFutures = stacks.stream()
                        .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                                stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), umsEventGenerationIds, fullSync,
                                        operationId, accountId)));
            } else {
                String deletedWorkloadUser = userSyncFilter.getDeletedWorkloadUser().get();
                statusFutures = stacks.stream()
                        .collect(Collectors.toMap(Stack::getEnvironmentCrn, stack -> asyncSynchronizeStackForDeleteUser(stack, deletedWorkloadUser)));
            }

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

    private FailureDetails createFailureDetails(String envCrn, String details, Multimap<String, String> warnings) {
        FailureDetails failureDetails = new FailureDetails(envCrn, details);
        Map<String, String> additionalDetails = failureDetails.getAdditionalDetails();

        warnings.asMap().entrySet().forEach(entry ->
                additionalDetails.put(
                        entry.getKey(),
                        entry.getValue().stream().collect(Collectors.joining(", "))));

        return failureDetails;
    }

    private Future<SyncStatusDetail> asyncSynchronizeStack(Stack stack, UmsUsersState umsUsersState, UmsEventGenerationIds umsEventGenerationIds,
            boolean fullSync, String operationId, String accountId) {
        return asyncTaskExecutor.submit(() -> {
            SyncStatusDetail statusDetail = internalSynchronizeStack(stack, umsUsersState, fullSync);
            if (fullSync && statusDetail.getStatus() == SynchronizationStatus.COMPLETED) {
                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                userSyncStatus.setUmsEventGenerationIds(new Json(umsEventGenerationIds));
                userSyncStatus.setLastSuccessfulFullSync(operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
                userSyncStatusService.save(userSyncStatus);
            }
            return statusDetail;
        });

    }

    private Future<SyncStatusDetail> asyncSynchronizeStackForDeleteUser(Stack stack, String deletedWorkloadUser) {
        return asyncTaskExecutor.submit(() -> internalSynchronizeStackForDeleteUser(stack, deletedWorkloadUser, false));
    }

    private SyncStatusDetail internalSynchronizeStack(Stack stack, UmsUsersState umsUsersState, boolean fullSync) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            boolean fmsToFreeipaBatchCallEnabled = entitlementService.isFmsToFreeipaBatchCallEnabled(Crn.fromString(environmentCrn).getAccountId());
            UsersStateDifference usersStateDifferenceBeforeSync = compareUmsAndFreeIpa(umsUsersState, fullSync, freeIpaClient);
            applyDifference(umsUsersState, environmentCrn, warnings, usersStateDifferenceBeforeSync, freeIpaClient, fmsToFreeipaBatchCallEnabled);

            retrySyncIfBatchCallHasWarnings(stack, umsUsersState, fullSync, warnings, freeIpaClient,
                    fmsToFreeipaBatchCallEnabled, usersStateDifferenceBeforeSync);

            // TODO For now we only sync cloud ids during full sync. We should eventually allow more granular syncs (actor level and group level sync).
            if (fullSync && entitlementService.cloudIdentityMappingEnabled(stack.getAccountId())) {
                LOGGER.debug("Starting {} ...", LogEvent.SYNC_CLOUD_IDENTITIES);
                cloudIdentitySyncService.syncCloudIdentities(stack, umsUsersState, warnings::put);
                LOGGER.debug("Finished {}.", LogEvent.SYNC_CLOUD_IDENTITIES);
            }

            return toSyncStatusDetail(environmentCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private void retrySyncIfBatchCallHasWarnings(Stack stack, UmsUsersState umsUsersState, boolean fullSync, Multimap<String, String> warnings,
            FreeIpaClient freeIpaClient, boolean fmsToFreeipaBatchCallEnabled, UsersStateDifference usersStateDifferenceBeforeSync)
            throws FreeIpaClientException, IOException {
        if (fullSync && !warnings.isEmpty() && fmsToFreeipaBatchCallEnabled) {
            UsersStateDifference usersStateDifferenceAfterSync = compareUmsAndFreeIpa(umsUsersState, fullSync, freeIpaClient);
            if (usersStateDifferenceChanged(usersStateDifferenceBeforeSync, usersStateDifferenceAfterSync)) {
                Multimap<String, String> retryWarnings = ArrayListMultimap.create();
                try {
                    LOGGER.info(String.format("Sync was partially successful for %s, thus we are trying it once again", stack.getResourceCrn()));
                    applyDifference(umsUsersState, stack.getEnvironmentCrn(), retryWarnings, usersStateDifferenceAfterSync,
                            freeIpaClient, fmsToFreeipaBatchCallEnabled);
                    warnings.clear();
                } finally {
                    warnings.putAll(retryWarnings);
                }
            }
        }
    }

    private boolean usersStateDifferenceChanged(UsersStateDifference beforeSync, UsersStateDifference afterSync) {
        return beforeSync.getUsersToAdd().size() != afterSync.getUsersToAdd().size() ||
                beforeSync.getUsersToRemove().size() != afterSync.getUsersToRemove().size() ||
                beforeSync.getGroupsToAdd().size() != afterSync.getUsersToAdd().size() ||
                beforeSync.getGroupsToRemove().size() != afterSync.getGroupsToRemove().size() ||
                beforeSync.getGroupMembershipToAdd().size() != afterSync.getGroupMembershipToAdd().size() ||
                beforeSync.getGroupMembershipToRemove().size() != afterSync.getGroupMembershipToRemove().size();
    }

    private UsersStateDifference compareUmsAndFreeIpa(UmsUsersState umsUsersState, boolean fullSync, FreeIpaClient freeIpaClient)
            throws FreeIpaClientException {
        LogEvent logEvent = fullSync ? LogEvent.RETRIEVE_FULL_IPA_STATE : LogEvent.RETRIEVE_PARTIAL_IPA_STATE;
        LOGGER.debug("Starting {} ...", logEvent);
        UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, fullSync);
        LOGGER.debug("Finished {}, found {} users and {} groups.", logEvent,
                ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

        LOGGER.debug("Starting {} ...", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);
        UsersStateDifference usersStateDifference = UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState);
        LOGGER.debug("Finished {}.", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);

        return usersStateDifference;
    }

    private void applyDifference(UmsUsersState umsUsersState, String environmentCrn, Multimap<String, String> warnings,
            UsersStateDifference usersStateDifference, FreeIpaClient freeIpaClient, boolean fmsToFreeipaBatchCallEnabled)
            throws FreeIpaClientException, IOException {
        LOGGER.debug("Starting {} ...", LogEvent.APPLY_DIFFERENCE_TO_IPA);
        applyStateDifferenceToIpa(environmentCrn, freeIpaClient, usersStateDifference, warnings::put, fmsToFreeipaBatchCallEnabled);
        LOGGER.debug("Finished {}.", LogEvent.APPLY_DIFFERENCE_TO_IPA);

        if (!FreeIpaCapabilities.hasSetPasswordHashSupport(freeIpaClient.getConfig())) {
            LOGGER.debug("IPA doesn't have password hash support, no credentials sync required for env:{}", environmentCrn);
        } else {
            // Sync credentials for all users and not just diff. At present there is no way to identify that there is a change in password for a user
            LOGGER.debug("Starting {} for {} users ...", LogEvent.SET_WORKLOAD_CREDENTIALS, umsUsersState.getUsersWorkloadCredentialMap().size());
            workloadCredentialService.setWorkloadCredentials(fmsToFreeipaBatchCallEnabled, freeIpaClient,
                    umsUsersState.getUsersWorkloadCredentialMap(), warnings::put);
            LOGGER.debug("Finished {}.", LogEvent.SET_WORKLOAD_CREDENTIALS);
        }
    }

    private SyncStatusDetail internalSynchronizeStackForDeleteUser(Stack stack, String deletedWorkloadUser, boolean fmsToFreeipaBatchCallEnabled) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);

            LOGGER.debug("Starting {} for environment {} and deleted user {} ...", LogEvent.USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);

            LOGGER.debug("Starting {} ...", LogEvent.RETRIEVE_PARTIAL_IPA_STATE);
            UsersState ipaUserState = getIpaStateForUser(freeIpaClient, deletedWorkloadUser);
            LOGGER.debug("Finished {}, found {} users and {} groups.", LogEvent.RETRIEVE_PARTIAL_IPA_STATE, ipaUserState.getUsers().size(),
                    ipaUserState.getGroups().size());

            if (!ipaUserState.getUsers().isEmpty()) {
                ImmutableCollection<String> groupsToRemove = ipaUserState.getGroupMembership().get(deletedWorkloadUser);
                UsersStateDifference usersStateDifference = UsersStateDifference.forDeletedUser(deletedWorkloadUser, groupsToRemove);
                LOGGER.debug("Starting {} ...", LogEvent.APPLY_DIFFERENCE_TO_IPA);
                applyStateDifferenceToIpa(stack.getEnvironmentCrn(), freeIpaClient, usersStateDifference, warnings::put, fmsToFreeipaBatchCallEnabled);
                LOGGER.debug("Finished {}.", LogEvent.APPLY_DIFFERENCE_TO_IPA);
            }

            LOGGER.debug("Finished {} for environment {} and deleted user {} ...", LogEvent.USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);
            return toSyncStatusDetail(environmentCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private SyncStatusDetail toSyncStatusDetail(String environmentCrn, Multimap<String, String> warnings) {
        if (warnings.isEmpty()) {
            return SyncStatusDetail.succeed(environmentCrn);
        } else {
            return SyncStatusDetail.fail(environmentCrn, "Synchronization completed with warnings.", warnings);
        }
    }

    @VisibleForTesting
    UsersState getIpaUserState(FreeIpaClient freeIpaClient, UmsUsersState umsUsersState, boolean fullSync)
            throws FreeIpaClientException {
        return fullSync ? freeIpaUsersStateProvider.getUsersState(freeIpaClient) :
                freeIpaUsersStateProvider.getFilteredFreeIpaState(
                        freeIpaClient, umsUsersState.getRequestedWorkloadUsernames());
    }

    @VisibleForTesting
    UsersState getIpaStateForUser(FreeIpaClient freeIpaClient, String workloadUserName) throws FreeIpaClientException {
        return freeIpaUsersStateProvider.getFilteredFreeIpaState(freeIpaClient, Set.of(workloadUserName));
    }

    @VisibleForTesting
    void applyStateDifferenceToIpa(String environmentCrn, FreeIpaClient freeIpaClient, UsersStateDifference stateDifference,
            BiConsumer<String, String> warnings, boolean fmsToFreeipaBatchCallEnabled) throws FreeIpaClientException {
        LOGGER.info("Applying state difference to environment {}.", environmentCrn);

        LOGGER.debug("Starting {} for {} groups ...", LogEvent.ADD_GROUPS,
                stateDifference.getGroupsToAdd().size());
        addGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupsToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_GROUPS);

        LOGGER.debug("Starting {} for {} users ...", LogEvent.ADD_USERS,
                stateDifference.getUsersToAdd().size());
        addUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_USERS);

        LOGGER.debug("Starting {} for {} group memberships ...", LogEvent.ADD_USERS_TO_GROUPS,
                stateDifference.getGroupMembershipToAdd().size());
        addUsersToGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupMembershipToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_USERS_TO_GROUPS);

        LOGGER.debug("Starting {} for {} group memberships ...", LogEvent.REMOVE_USERS_FROM_GROUPS,
                stateDifference.getGroupMembershipToRemove().size());
        removeUsersFromGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupMembershipToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_USERS_FROM_GROUPS);

        LOGGER.debug("Starting {} for {} users ...", LogEvent.REMOVE_USERS,
                stateDifference.getUsersToRemove().size());
        removeUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_USERS);

        LOGGER.debug("Starting {} for {} groups ...", LogEvent.REMOVE_GROUPS,
                stateDifference.getGroupsToRemove().size());
        removeGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupsToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_GROUPS);
    }

    void addGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<GroupAddOperation> operations = Lists.newArrayList();
        for (FmsGroup fmsGroup : fmsGroups) {
            operations.add(GroupAddOperation.create(fmsGroup.getName(), warnings));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), false);
    }

    void addUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsUser> fmsUsers,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<UserAddOperation> operations = Lists.newArrayList();
        for (FmsUser fmsUser : fmsUsers) {
            operations.add(UserAddOperation.create(fmsUser.getName(), fmsUser.getFirstName(), fmsUser.getLastName()));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), true);
    }

    void removeUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> fmsUsers,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<UserRemoveOperation> operations = Lists.newArrayList();
        for (String user : fmsUsers) {
            operations.add(UserRemoveOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.NOT_FOUND), true);
    }

    void removeGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<GroupRemoveOperation> operations = Lists.newArrayList();
        for (FmsGroup fmsGroup : fmsGroups) {
            operations.add(GroupRemoveOperation.create(fmsGroup.getName(), warnings));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.NOT_FOUND), false);
    }

    void addUsersToGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<GroupAddMemberOperation> operations = Lists.newArrayList();
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                operations.add(GroupAddMemberOperation.create(group, users, warnings));
            }
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(), false);
    }

    void removeUsersFromGroups(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<GroupRemoveMemberOperation> operations = Lists.newArrayList();
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                operations.add(GroupRemoveMemberOperation.create(group, users, warnings));
            }
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(), false);
    }

    private <T extends AbstractFreeipaOperation> void invokeOperation(List<T> operations, boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeipaClient,
            BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes, boolean localErrorHandling)
            throws FreeIpaClientException {
        if (fmsToFreeipaBatchCallEnabled) {
            List<Object> batchCallOperations = operations.stream().map(operation -> operation.getOperationParamsForBatchCall()).collect(Collectors.toList());
            String operationName = operations.stream().map(op -> op.getOperationName()).findFirst().orElse("unknown");
            Integer partitionSize = batchPartitionSizeProperties.getByOperation(operationName);
            freeipaClient.callBatch(warnings, batchCallOperations, partitionSize, acceptableErrorCodes);
        } else {
            for (T operation : operations) {
                try {
                    operation.invoke(freeipaClient);
                } catch (FreeIpaClientException e) {
                    singleOperationErrorHandling(freeipaClient, warnings, acceptableErrorCodes, localErrorHandling, operation, e);
                }
            }
        }
    }

    private void singleOperationErrorHandling(FreeIpaClient freeipaClient, BiConsumer<String, String> warnings, Set<FreeIpaErrorCodes> acceptableErrorCodes,
            boolean localErrorHandling, AbstractFreeipaOperation operation, FreeIpaClientException e) throws FreeIpaClientException {
        if (localErrorHandling) {
            if (FreeIpaClientExceptionUtil.isExceptionWithErrorCode(e, acceptableErrorCodes)) {
                LOGGER.debug(String.format("Operation %s failed with acceptable error: %s", operation.getOperationName(), e.getMessage()));
            } else {
                LOGGER.warn(e.getMessage());
                warnings.accept(String.format("operation %s failed", operation.getOperationName()), e.getMessage());
                freeipaClient.checkIfClientStillUsable(e);
            }
        } else {
            throw e;
        }
    }

    private Set<String> union(Collection<String> collection1, Collection<String> collection2) {
        return Stream.concat(collection1.stream(), collection2.stream()).collect(Collectors.toSet());
    }
}