package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.client.FreeIpaGroupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
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
import com.sequenceiq.freeipa.client.operation.UserDisableOperation;
import com.sequenceiq.freeipa.client.operation.UserEnableOperation;
import com.sequenceiq.freeipa.client.operation.UserRemoveOperation;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
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
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;
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
        REMOVE_GROUPS,
        DISABLE_USERS,
        ENABLE_USERS
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
    private ExecutorService asyncTaskExecutor;

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

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Operation synchronizeUsers(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, WorkloadCredentialsUpdateType workloadCredentialsUpdateType) {
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(userCrnFilter, machineUserCrnFilter, Optional.empty());
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userSyncFilter);
        UserSyncOptions options = getUserSyncOptions(accountId, userSyncFilter.isFullSync(), workloadCredentialsUpdateType);
        return performSyncForStacks(accountId, userSyncFilter, options, stacks);
    }

    public Operation synchronizeUsersWithCustomPermissionCheck(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            UserSyncRequestFilter userSyncFilter, WorkloadCredentialsUpdateType workloadCredentialsUpdateType, AuthorizationResourceAction action) {
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userSyncFilter);
        List<String> relatedEnvironmentCrns = stacks.stream().map(stack -> stack.getEnvironmentCrn()).collect(Collectors.toList());
        CustomCheckUtil.run(actorCrn, () -> commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, actorCrn, relatedEnvironmentCrns));
        UserSyncOptions options = getUserSyncOptions(accountId, userSyncFilter.isFullSync(), workloadCredentialsUpdateType);
        return performSyncForStacks(accountId, userSyncFilter, options, stacks);
    }

    private UserSyncOptions getUserSyncOptions(String accountId, boolean fullSync, WorkloadCredentialsUpdateType requestedCredentialsUpdateType) {
        WorkloadCredentialsUpdateType credentialsUpdateType = requestedCredentialsUpdateType == WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED &&
                !entitlementService.usersyncCredentialsUpdateOptimizationEnabled(accountId) ?
                WorkloadCredentialsUpdateType.FORCE_UPDATE : requestedCredentialsUpdateType;
        UserSyncOptions userSyncOptions = new UserSyncOptions(fullSync, entitlementService.isFmsToFreeipaBatchCallEnabled(accountId),
                credentialsUpdateType);
        LOGGER.info("Credentials update optimization is{} enabled for this sync request",
                userSyncOptions.isCredentialsUpdateOptimizationEnabled() ? "" : " not");
        return userSyncOptions;
    }

    private Operation performSyncForStacks(String accountId, UserSyncRequestFilter userSyncFilter, UserSyncOptions options,
            List<Stack> stacks) {
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
                    ThreadBasedUserCrnProvider.doAs(
                            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(), () -> {
                        boolean fullSync = userSyncFilter.isFullSync();
                        if (fullSync) {
                            stacks.forEach(stack -> {
                                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                                userSyncStatus.setLastStartedFullSync(operation);
                                userSyncStatusService.save(userSyncStatus);
                            });
                        }
                        asyncSynchronizeUsers(operation.getOperationId(), accountId, stacks, userSyncFilter, options);
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
    void asyncSynchronizeUsers(String operationId, String accountId, List<Stack> stacks, UserSyncRequestFilter userSyncFilter,
            UserSyncOptions options) {
        try {
            MDCBuilder.addOperationId(operationId);
            asyncTaskExecutor.submit(() -> internalSynchronizeUsers(operationId, accountId, stacks, userSyncFilter, options));
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

    private void internalSynchronizeUsers(String operationId, String accountId, List<Stack> stacks, UserSyncRequestFilter userSyncFilter,
            UserSyncOptions options) {
        tryWithOperationCleanup(operationId, accountId, () -> {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());

            Optional<String> requestId = MDCUtils.getRequestId();

            UmsEventGenerationIds umsEventGenerationIds = options.isFullSync() ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId) :
                    null;

            LogEvent logUserSyncEvent = options.isFullSync() ? LogEvent.FULL_USER_SYNC : LogEvent.PARTIAL_USER_SYNC;
            LOGGER.info("Starting {} for environments {} with operationId {} ...", logUserSyncEvent, environmentCrns, operationId);

            Map<String, Future<SyncStatusDetail>> statusFutures;

            if (userSyncFilter.getDeletedWorkloadUser().isEmpty()) {
                LogEvent logRetrieveUmsEvent = options.isFullSync() ? LogEvent.RETRIEVE_FULL_UMS_STATE : LogEvent.RETRIEVE_PARTIAL_UMS_STATE;
                LOGGER.debug("Starting {} for environments {} ...", logRetrieveUmsEvent, environmentCrns);
                Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProviderDispatcher
                        .getEnvToUmsUsersStateMap(accountId, environmentCrns, userSyncFilter.getUserCrnFilter(),
                                userSyncFilter.getMachineUserCrnFilter(), requestId);
                LOGGER.debug("Finished {}.", logRetrieveUmsEvent);
                statusFutures = stacks.stream()
                        .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                                stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), umsEventGenerationIds, options,
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
            UserSyncOptions options, String operationId, String accountId) {
        return asyncTaskExecutor.submit(() -> {
            SyncStatusDetail statusDetail = internalSynchronizeStack(stack, umsUsersState, options);
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
        return asyncTaskExecutor.submit(() -> internalSynchronizeStackForDeleteUser(stack, deletedWorkloadUser, false));
    }

    private SyncStatusDetail internalSynchronizeStack(Stack stack, UmsUsersState umsUsersState, UserSyncOptions options) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            UsersStateDifference usersStateDifferenceBeforeSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient);
            applyDifference(umsUsersState, environmentCrn, warnings, usersStateDifferenceBeforeSync, options, freeIpaClient);

            retrySyncIfBatchCallHasWarnings(stack, umsUsersState, warnings, options, freeIpaClient, usersStateDifferenceBeforeSync);

            // TODO For now we only sync cloud ids during full sync. We should eventually allow more granular syncs (actor level and group level sync).
            if (options.isFullSync() && entitlementService.cloudIdentityMappingEnabled(stack.getAccountId())) {
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

    private void retrySyncIfBatchCallHasWarnings(Stack stack, UmsUsersState umsUsersState, Multimap<String, String> warnings,
            UserSyncOptions options, FreeIpaClient freeIpaClient, UsersStateDifference usersStateDifferenceBeforeSync)
            throws FreeIpaClientException {
        if (options.isFullSync() && !warnings.isEmpty() && options.isFmsToFreeIpaBatchCallEnabled()) {
            UsersStateDifference usersStateDifferenceAfterSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient);
            if (usersStateDifferenceChanged(usersStateDifferenceBeforeSync, usersStateDifferenceAfterSync)) {
                Multimap<String, String> retryWarnings = ArrayListMultimap.create();
                try {
                    LOGGER.info(String.format("Sync was partially successful for %s, thus we are trying it once again", stack.getResourceCrn()));
                    applyDifference(umsUsersState, stack.getEnvironmentCrn(), retryWarnings, usersStateDifferenceAfterSync, options, freeIpaClient);
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
                beforeSync.getGroupMembershipToRemove().size() != afterSync.getGroupMembershipToRemove().size() ||
                beforeSync.getUsersWithCredentialsToUpdate().size() != afterSync.getUsersWithCredentialsToUpdate().size();
    }

    private UsersStateDifference compareUmsAndFreeIpa(UmsUsersState umsUsersState, UserSyncOptions options,
            FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        LogEvent logEvent = options.isFullSync() ? LogEvent.RETRIEVE_FULL_IPA_STATE : LogEvent.RETRIEVE_PARTIAL_IPA_STATE;
        LOGGER.debug("Starting {} ...", logEvent);
        UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, options.isFullSync());
        LOGGER.debug("Finished {}, found {} users and {} groups.", logEvent,
                ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

        LOGGER.debug("Starting {} ...", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);
        UsersStateDifference usersStateDifference = UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState, options);
        LOGGER.debug("Finished {}.", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);

        return usersStateDifference;
    }

    private void applyDifference(UmsUsersState umsUsersState, String environmentCrn, Multimap<String, String> warnings,
            UsersStateDifference usersStateDifference, UserSyncOptions options, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        LOGGER.debug("Starting {} ...", LogEvent.APPLY_DIFFERENCE_TO_IPA);
        applyStateDifferenceToIpa(environmentCrn, freeIpaClient, usersStateDifference, warnings::put, options.isFmsToFreeIpaBatchCallEnabled());
        LOGGER.debug("Finished {}.", LogEvent.APPLY_DIFFERENCE_TO_IPA);

        if (!FreeIpaCapabilities.hasSetPasswordHashSupport(freeIpaClient.getConfig())) {
            LOGGER.debug("IPA doesn't have password hash support, no credentials sync required for env:{}", environmentCrn);
        } else {
            LOGGER.debug("Starting {} for {} users ...", LogEvent.SET_WORKLOAD_CREDENTIALS, usersStateDifference.getUsersWithCredentialsToUpdate().size());
            ImmutableSet<WorkloadCredentialUpdate> credentialUpdates = usersStateDifference.getUsersWithCredentialsToUpdate().stream()
                    .map(username -> getCredentialUpdate(username, umsUsersState))
                    .collect(ImmutableSet.toImmutableSet());
            workloadCredentialService.setWorkloadCredentials(options, freeIpaClient, credentialUpdates, warnings::put);
            LOGGER.debug("Finished {}.", LogEvent.SET_WORKLOAD_CREDENTIALS);
        }
    }

    private WorkloadCredentialUpdate getCredentialUpdate(String username, UmsUsersState umsUsersState) {
        UserMetadata userMetadata = requireNonNull(umsUsersState.getUsersState().getUserMetadataMap().get(username),
                "userMetadata must not be null");
        WorkloadCredential workloadCredential = requireNonNull(umsUsersState.getUsersWorkloadCredentialMap().get(username),
                "workloadCredential must not be null");
        return new WorkloadCredentialUpdate(username, userMetadata.getCrn(), workloadCredential);
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
                ImmutableCollection<String> groupMembershipsToRemove = ipaUserState.getGroupMembership().get(deletedWorkloadUser);
                UsersStateDifference usersStateDifference = UsersStateDifference.forDeletedUser(deletedWorkloadUser, groupMembershipsToRemove);
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
        LOGGER.info("Applying state difference {} to environment {}.", stateDifference, environmentCrn);

        LOGGER.debug("Starting {} for {} groups ...", LogEvent.ADD_GROUPS,
                stateDifference.getGroupsToAdd().size());
        addGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupsToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_GROUPS);

        LOGGER.debug("Starting {} for {} users ...", LogEvent.ADD_USERS,
                stateDifference.getUsersToAdd().size());
        addUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_USERS);

        LOGGER.debug("Starting {} for {} users ...", LogEvent.DISABLE_USERS,
                stateDifference.getUsersToDisable().size());
        disableUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToDisable(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.DISABLE_USERS);

        LOGGER.debug("Starting {} for {} users ...", LogEvent.ENABLE_USERS,
                stateDifference.getUsersToEnable().size());
        enableUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToEnable(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ENABLE_USERS);

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
        List<GroupAddOperation> posixOperations = Lists.newArrayList();
        List<GroupAddOperation> nonPosixOperations = Lists.newArrayList();
        for (FmsGroup fmsGroup : fmsGroups) {
            String groupName = fmsGroup.getName();
            if (isNonPosixGroup(groupName)) {
                nonPosixOperations.add(GroupAddOperation.create(groupName, FreeIpaGroupType.NONPOSIX, warnings));
            } else {
                posixOperations.add(GroupAddOperation.create(groupName, FreeIpaGroupType.POSIX, warnings));
            }
        }
        invokeOperation(posixOperations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), false);
        invokeOperation(nonPosixOperations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), false);
    }

    private boolean isNonPosixGroup(String groupName) {
        return UserSyncConstants.NON_POSIX_GROUPS.contains(groupName);
    }

    void addUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<FmsUser> fmsUsers,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<UserAddOperation> operations = Lists.newArrayList();
        for (FmsUser fmsUser : fmsUsers) {
            operations.add(UserAddOperation.create(fmsUser.getName(), fmsUser.getFirstName(), fmsUser.getLastName(),
                    fmsUser.getState() == FmsUser.State.DISABLED));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY), true);
    }

    void disableUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> users,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<UserDisableOperation> operations = Lists.newArrayList();
        for (String user : users) {
            operations.add(UserDisableOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.ALREADY_INACTIVE), true);
    }

    void enableUsers(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Set<String> users,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        List<UserEnableOperation> operations = Lists.newArrayList();
        for (String user : users) {
            operations.add(UserEnableOperation.create(user));
        }
        invokeOperation(operations, fmsToFreeipaBatchCallEnabled, freeIpaClient, warnings,
                Set.of(FreeIpaErrorCodes.ALREADY_ACTIVE), true);
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