package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaCapabilities;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
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
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UserSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncService.class);

    private enum LogEvent {
        FULL_USER_SYNC,
        PARTIAL_USER_SYNC,
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
    private UmsUsersStateProvider umsUsersStateProvider;

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

    public Operation synchronizeUsers(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);
        return performSyncForStacks(accountId, actorCrn, userCrnFilter, machineUserCrnFilter, stacks);
    }

    public Operation synchronizeUsersWithCustomPermissionCheck(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, AuthorizationResourceAction action) {
        List<Stack> stacks = getStacksForSync(accountId, actorCrn, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);
        List<String> relatedEnvironmentCrns = stacks.stream().map(stack -> stack.getEnvironmentCrn()).collect(Collectors.toList());
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, actorCrn, relatedEnvironmentCrns);
        return performSyncForStacks(accountId, actorCrn, userCrnFilter, machineUserCrnFilter, stacks);
    }

    private Operation performSyncForStacks(String accountId, String actorCrn, Set<String> userCrnFilter,
            Set<String> machineUserCrnFilter, List<Stack> stacks) {
        Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());
        Operation operation = operationService
                .startOperation(accountId, OperationType.USER_SYNC, environmentCrns, union(userCrnFilter, machineUserCrnFilter));

        String operationId = operation.getOperationId();
        OperationState operationState = operation.getStatus();
        LOGGER.info("Starting operation [{}] with status [{}]", operationId, operationState);

        if (operationState == OperationState.RUNNING) {
            tryWithOperationCleanup(operationId, accountId, () ->
                    ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> {
                        boolean fullSync = userCrnFilter.isEmpty() && machineUserCrnFilter.isEmpty();
                        if (fullSync) {
                            stacks.forEach(stack -> {
                                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                                userSyncStatus.setLastStartedFullSync(operation);
                                userSyncStatusService.save(userSyncStatus);
                            });
                        }
                        asyncSynchronizeUsers(operation.getOperationId(), accountId, actorCrn, stacks, userCrnFilter, machineUserCrnFilter, fullSync);
                    }));
        }

        return operation;
    }

    private List<Stack> getStacksForSync(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        validateParameters(accountId, actorCrn, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);
        LOGGER.debug("Synchronizing users in account {} for environmentCrns {}, userCrns {}, and machineUserCrns {}",
                accountId, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);

        List<Stack> stacks = stackService.getMultipleByEnvironmentCrnOrChildEnvironmantCrnAndAccountId(environmentCrnFilter, accountId);
        if (stacks.isEmpty()) {
            throw new NotFoundException(String.format("No matching FreeIPA stacks found for account %s with environment crn filter %s",
                    accountId, environmentCrnFilter));
        }
        LOGGER.debug("Found {} stacks", stacks.size());
        return stacks;
    }

    @VisibleForTesting
    void asyncSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, boolean fullSync) {

        try {
            MDCBuilder.addOperationId(operationId);
            asyncTaskExecutor.submit(() -> internalSynchronizeUsers(
                    operationId, accountId, actorCrn, stacks, userCrnFilter, machineUserCrnFilter, fullSync));
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

    private void internalSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, boolean fullSync) {
        tryWithOperationCleanup(operationId, accountId, () -> {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());

            Optional<String> requestId = MDCUtils.getRequestId();

            UmsEventGenerationIds umsEventGenerationIds = fullSync ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId) :
                    null;

            LogEvent logUserSyncEvent = fullSync ? LogEvent.FULL_USER_SYNC : LogEvent.PARTIAL_USER_SYNC;
            LOGGER.info("Starting {} for environments {} with operationId {} ...", logUserSyncEvent, environmentCrns, operationId);
            LogEvent logRetrieveUmsEvent = fullSync ? LogEvent.RETRIEVE_FULL_UMS_STATE : LogEvent.RETRIEVE_PARTIAL_UMS_STATE;
            LOGGER.debug("Starting {} for environments {} ...", logRetrieveUmsEvent, environmentCrns);
            Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProvider
                    .getEnvToUmsUsersStateMap(accountId, actorCrn, environmentCrns, userCrnFilter, machineUserCrnFilter, requestId);
            LOGGER.debug("Finished {}.", logRetrieveUmsEvent);

            List<SuccessDetails> success = new ArrayList<>();
            List<FailureDetails> failure = new ArrayList<>();

            Map<String, Future<SyncStatusDetail>> statusFutures = stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                            stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), umsEventGenerationIds, fullSync,
                                    operationId, accountId)));

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

    private SyncStatusDetail internalSynchronizeStack(Stack stack, UmsUsersState umsUsersState, boolean fullSync) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            LogEvent logEvent = fullSync ? LogEvent.RETRIEVE_FULL_IPA_STATE : LogEvent.RETRIEVE_PARTIAL_IPA_STATE;
            LOGGER.debug("Starting {} ...", logEvent);
            UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, fullSync);
            LOGGER.debug("Finished {}, found {} users and {} groups.", logEvent,
                    ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

            LOGGER.debug("Starting {} ...", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);
            UsersStateDifference usersStateDifference = UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState);
            LOGGER.debug("Finished {}.", LogEvent.CALCULATE_UMS_IPA_DIFFERENCE);

            LOGGER.debug("Starting {} ...", LogEvent.APPLY_DIFFERENCE_TO_IPA);
            applyStateDifferenceToIpa(stack.getEnvironmentCrn(), freeIpaClient, usersStateDifference, warnings::put);
            LOGGER.debug("Finished {}.", LogEvent.APPLY_DIFFERENCE_TO_IPA);

            if (!FreeIpaCapabilities.hasSetPasswordHashSupport(freeIpaClient.getConfig())) {
                LOGGER.debug("IPA doesn't have password hash support, no credentials sync required for env:{}", environmentCrn);
            } else {
                // Sync credentials for all users and not just diff. At present there is no way to identify that there is a change in password for a user
                LOGGER.debug("Starting {} for {} users ...", LogEvent.SET_WORKLOAD_CREDENTIALS, umsUsersState.getUsersWorkloadCredentialMap().size());
                workloadCredentialService.setWorkloadCredentials(freeIpaClient, umsUsersState.getUsersWorkloadCredentialMap(), warnings::put);
                LOGGER.debug("Finished {}.", LogEvent.SET_WORKLOAD_CREDENTIALS);
            }

            // TODO For now we only sync cloud ids during full sync. We should eventually allow more granular syncs (actor level and group level sync).
            if (fullSync && entitlementService.cloudIdentityMappingEnabled(INTERNAL_ACTOR_CRN, stack.getAccountId())) {
                LOGGER.debug("Starting {} ...", LogEvent.SYNC_CLOUD_IDENTITIES);
                cloudIdentitySyncService.syncCloudIdentites(stack, umsUsersState, warnings::put);
                LOGGER.debug("Finished {}.", LogEvent.SYNC_CLOUD_IDENTITIES);
            }

            if (warnings.isEmpty()) {
                return SyncStatusDetail.succeed(environmentCrn);
            } else {
                return SyncStatusDetail.fail(environmentCrn, "Synchronization completed with warnings.", warnings);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    @VisibleForTesting
    UsersState getIpaUserState(FreeIpaClient freeIpaClient, UmsUsersState umsUsersState, boolean fullSync)
            throws FreeIpaClientException {
        return fullSync ? freeIpaUsersStateProvider.getUsersState(freeIpaClient) :
                freeIpaUsersStateProvider.getFilteredFreeIpaState(freeIpaClient, umsUsersState.getRequestedWorkloadUsers());
    }

    @VisibleForTesting
    void applyStateDifferenceToIpa(String environmentCrn, FreeIpaClient freeIpaClient, UsersStateDifference stateDifference,
                    BiConsumer<String, String> warnings) throws FreeIpaClientException {
        LOGGER.info("Applying state difference to environment {}.", environmentCrn);

        LOGGER.debug("Starting {} ...", LogEvent.ADD_GROUPS);
        addGroups(freeIpaClient, stateDifference.getGroupsToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_GROUPS);

        LOGGER.debug("Starting {} ...", LogEvent.ADD_USERS);
        addUsers(freeIpaClient, stateDifference.getUsersToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_USERS);

        LOGGER.debug("Starting {} ...", LogEvent.ADD_USERS_TO_GROUPS);
        addUsersToGroups(freeIpaClient, stateDifference.getGroupMembershipToAdd(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.ADD_USERS_TO_GROUPS);

        LOGGER.debug("Starting {} ...", LogEvent.REMOVE_USERS_FROM_GROUPS);
        removeUsersFromGroups(freeIpaClient, stateDifference.getGroupMembershipToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_USERS_FROM_GROUPS);

        LOGGER.debug("Starting {} ...", LogEvent.REMOVE_USERS);
        removeUsers(freeIpaClient, stateDifference.getUsersToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_USERS);

        LOGGER.debug("Starting {} ...", LogEvent.REMOVE_GROUPS);
        removeGroups(freeIpaClient, stateDifference.getGroupsToRemove(), warnings);
        LOGGER.debug("Finished {}.", LogEvent.REMOVE_GROUPS);
    }

    private void addGroups(FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups, BiConsumer<String, String> warnings) throws FreeIpaClientException {
        for (FmsGroup fmsGroup : fmsGroups) {
            LOGGER.debug("adding group {}", fmsGroup.getName());
            try {
                com.sequenceiq.freeipa.client.model.Group groupAdd = freeIpaClient.groupAdd(fmsGroup.getName());
                LOGGER.debug("Success: {}", groupAdd);
            } catch (FreeIpaClientException e) {
                if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                    LOGGER.debug("group '{}' already exists", fmsGroup.getName());
                } else {
                    LOGGER.warn("Failed to add group {}", fmsGroup.getName(), e);
                    warnings.accept(fmsGroup.getName(), "Failed to add group:" + e.getMessage());
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    private void addUsers(FreeIpaClient freeIpaClient, Set<FmsUser> fmsUsers, BiConsumer<String, String> warnings) throws FreeIpaClientException {
        for (FmsUser fmsUser : fmsUsers) {
            String username = fmsUser.getName();
            LOGGER.debug("adding user {}", username);
            try {
                com.sequenceiq.freeipa.client.model.User userAdd = freeIpaClient.userAdd(
                        username, fmsUser.getFirstName(), fmsUser.getLastName());
                LOGGER.debug("Success: {}", userAdd);
            } catch (FreeIpaClientException e) {
                if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                    LOGGER.debug("user '{}' already exists", fmsUser.getName());
                } else {
                    LOGGER.error("Failed to add {}", username, e);
                    warnings.accept(fmsUser.getName(), "Failed to add user:" + e.getMessage());
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    private void removeUsers(FreeIpaClient freeIpaClient, Set<String> fmsUsers, BiConsumer<String, String> warnings) throws FreeIpaClientException {
        for (String username : fmsUsers) {
            LOGGER.debug("Removing user {}", username);
            try {
                com.sequenceiq.freeipa.client.model.User userRemove = freeIpaClient.deleteUser(username);
                LOGGER.debug("Success: {}", userRemove);
            } catch (FreeIpaClientException e) {
                if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                    LOGGER.debug("user '{}' already does not exists", username);
                } else {
                    LOGGER.error("Failed to delete {}", username, e);
                    warnings.accept(username, "Failed to remove user:" + e.getMessage());
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    private void removeGroups(FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups, BiConsumer<String, String> warnings) throws FreeIpaClientException {
        for (FmsGroup fmsGroup : fmsGroups) {
            String groupname = fmsGroup.getName();

            LOGGER.debug("Removing group {}", groupname);

            try {
                freeIpaClient.deleteGroup(groupname);
                LOGGER.debug("Success: {}", groupname);
            } catch (FreeIpaClientException e) {
                if (FreeIpaClientExceptionUtil.isNotFoundException(e)) {
                    LOGGER.debug("group '{}' already does not exists", groupname);
                } else {
                    LOGGER.error("Failed to delete {}", groupname, e);
                    warnings.accept(groupname, "Failed to remove group: " + e.getMessage());
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    @VisibleForTesting
    void addUsersToGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping, BiConsumer<String, String> warnings)
            throws FreeIpaClientException {
        LOGGER.debug("adding users to groups: [{}]", groupMapping);
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                LOGGER.debug("adding users [{}] to group [{}]", users, group);
                try {
                    RPCResponse<Group> groupAddMemberResponse = freeIpaClient.groupAddMembers(group, users);
                    List<String> members = Optional.ofNullable(groupAddMemberResponse.getResult().getMemberUser()).orElse(List.of());
                    if (members.containsAll(users)) {
                        LOGGER.debug("Successfully added users {} to {}", users, groupAddMemberResponse.getResult());
                    } else {
                        // TODO specialize RPCResponse completed/failed objects
                        LOGGER.error("Failed to add {} to group '{}': {}", users, group, groupAddMemberResponse.getFailed());
                        warnings.accept(group, String.format("Failed to add users to group: %s", groupAddMemberResponse.getFailed()));
                    }
                } catch (FreeIpaClientException e) {
                    LOGGER.error("Failed to add {} to group '{}'", users, group, e);
                    warnings.accept(group, String.format("Failed to add users %s to group: %s", users, e.getMessage()));
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    @VisibleForTesting
    void removeUsersFromGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping, BiConsumer<String, String> warnings)
            throws FreeIpaClientException {
        for (String group : groupMapping.keySet()) {
            for (List<String> users : Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest)) {
                LOGGER.debug("removing users {} from group {}", users, group);
                try {
                    RPCResponse<Group> groupRemoveMembersResponse = freeIpaClient.groupRemoveMembers(group, users);
                    List<String> members = Optional.ofNullable(groupRemoveMembersResponse.getResult().getMemberUser()).orElse(List.of());
                    if (Collections.disjoint(members, users)) {
                        LOGGER.debug("Successfully removed users {} from {}", users, groupRemoveMembersResponse.getResult());
                    } else {
                        // TODO specialize RPCResponse completed/failed objects
                        LOGGER.error("Failed to remove {} from group '{}': {}", users, group, groupRemoveMembersResponse.getFailed());
                        warnings.accept(group, String.format("Failed to remove users from group: %s", groupRemoveMembersResponse.getFailed()));
                    }
                } catch (FreeIpaClientException e) {
                    LOGGER.error("Failed to remove {} from group '{}'", users, group, e);
                    warnings.accept(group, String.format("Failed to remove users %s from group: %s", users, e.getMessage()));
                    checkIfClientStillUsable(e);
                }
            }
        }
    }

    private void checkIfClientStillUsable(FreeIpaClientException e) throws FreeIpaClientException {
        if (e.isClientUnusable()) {
            LOGGER.warn("Client is not usable for further usage");
            throw e;
        }
    }

    @VisibleForTesting
    void validateParameters(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        requireNonNull(accountId, "accountId must not be null");
        requireNonNull(actorCrn, "actorCrn must not be null");
        requireNonNull(environmentCrnFilter, "environmentCrnFilter must not be null");
        requireNonNull(userCrnFilter, "userCrnFilter must not be null");
        requireNonNull(machineUserCrnFilter, "machineUserCrnFilter must not be null");
        validateCrnFilter(environmentCrnFilter, Crn.ResourceType.ENVIRONMENT);
        validateCrnFilter(userCrnFilter, Crn.ResourceType.USER);
        validateCrnFilter(machineUserCrnFilter, Crn.ResourceType.MACHINE_USER);
        validateSameAccount(accountId, Iterables.concat(environmentCrnFilter, userCrnFilter, machineUserCrnFilter));
    }

    private void validateSameAccount(String accountId, Iterable<String> crns) {
        crns.forEach(crnString -> {
            Crn crn = Crn.safeFromString(crnString);
            if (!accountId.equals(crn.getAccountId())) {
                throw new BadRequestException(String.format("Crn %s is not in the expected account %s", crnString, accountId));
            }
        });
    }

    @VisibleForTesting
    void validateCrnFilter(Set<String> crnFilter, Crn.ResourceType resourceType) {
        crnFilter.forEach(crnString -> {
            try {
                Crn crn = Crn.safeFromString(crnString);
                if (crn.getResourceType() != resourceType) {
                    throw new BadRequestException(String.format("Crn %s is not of expected type %s", crnString, resourceType));
                }
            } catch (CrnParseException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        });
    }

    private Set<String> union(Collection<String> collection1, Collection<String> collection2) {
        return Stream.concat(collection1.stream(), collection2.stream()).collect(Collectors.toSet());
    }
}
