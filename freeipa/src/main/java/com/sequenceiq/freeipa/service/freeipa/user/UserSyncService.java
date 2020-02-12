package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.KrbKeySetEncoder;

@Service
public class UserSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncService.class);

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

    public Operation synchronizeUsers(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {

        validateParameters(accountId, actorCrn, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);
        LOGGER.debug("Synchronizing users in account {} for environmentCrns {}, userCrns {}, and machineUserCrns {}",
                accountId, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);

        List<Stack> stacks = stackService.getMultipleByEnvironmentCrnAndAccountId(environmentCrnFilter, accountId);
        LOGGER.debug("Found {} stacks", stacks.size());
        if (stacks.isEmpty()) {
            throw new NotFoundException(String.format("No matching FreeIPA stacks found for account %s with environment crn filter %s",
                    accountId, environmentCrnFilter));
        }

        Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());
        Operation operation = operationService
                .startOperation(accountId, OperationType.USER_SYNC, environmentCrns, union(userCrnFilter, machineUserCrnFilter));

        LOGGER.info("Starting operation [{}] with status [{}]", operation.getOperationId(), operation.getStatus());

        if (operation.getStatus() == OperationState.RUNNING) {
            boolean fullSync = userCrnFilter.isEmpty() && machineUserCrnFilter.isEmpty();
            if (fullSync) {
                long currentTime = Instant.now().toEpochMilli();
                stacks.forEach(stack -> {
                    UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                    userSyncStatus.setLastFullSyncStartTime(currentTime);
                    userSyncStatusService.save(userSyncStatus);
                });
            }
            asyncSynchronizeUsers(operation.getOperationId(), accountId, actorCrn, stacks, userCrnFilter, machineUserCrnFilter, fullSync);
        }

        return operation;
    }

    private void asyncSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, boolean fullSync) {

        MDCBuilder.addFlowId(operationId);
        asyncTaskExecutor.submit(() -> internalSynchronizeUsers(
                operationId, accountId, actorCrn, stacks, userCrnFilter, machineUserCrnFilter, fullSync));

    }

    private void internalSynchronizeUsers(String operationId, String accountId, String actorCrn, List<Stack> stacks,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter, boolean fullSync) {
        try {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());

            Optional<String> requestId = MDCUtils.getRequestId();

            UmsEventGenerationIds umsEventGenerationIds = fullSync ?
                    umsEventGenerationIdsProvider.getEventGenerationIds(accountId, requestId) :
                    null;

            Map<String, UmsUsersState> envToUmsStateMap = umsUsersStateProvider
                    .getEnvToUmsUsersStateMap(accountId, actorCrn, environmentCrns, userCrnFilter, machineUserCrnFilter, requestId);

            List<SuccessDetails> success = new ArrayList<>();
            List<FailureDetails> failure = new ArrayList<>();

            Map<String, Future<SyncStatusDetail>> statusFutures = stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                            stack -> asyncSynchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), umsEventGenerationIds, fullSync)));

            statusFutures.forEach((envCrn, statusFuture) -> {
                try {
                    SyncStatusDetail statusDetail = statusFuture.get();
                    switch (statusDetail.getStatus()) {
                        case COMPLETED:
                            success.add(new SuccessDetails(envCrn));
                            break;
                        case FAILED:
                            failure.add(new FailureDetails(envCrn, statusDetail.getDetails()));
                            break;
                        default:
                            failure.add(new FailureDetails(envCrn, "Unexpected status: " + statusDetail.getStatus()));
                            break;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Sync is interrupted for env: {}", envCrn, e);
                    failure.add(new FailureDetails(envCrn, e.getLocalizedMessage()));
                }
            });
            operationService.completeOperation(accountId, operationId, success, failure);
        } catch (RuntimeException e) {
            LOGGER.error("User sync operation {} failed with error:", operationId, e);
            operationService.failOperation(accountId, operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    private Future<SyncStatusDetail> asyncSynchronizeStack(Stack stack, UmsUsersState umsUsersState, UmsEventGenerationIds umsEventGenerationIds,
            boolean fullSync) {
        return asyncTaskExecutor.submit(() -> {
            SyncStatusDetail statusDetail = internalSynchronizeStack(stack, umsUsersState, fullSync);
            if (fullSync && statusDetail.getStatus() == SynchronizationStatus.COMPLETED) {
                UserSyncStatus userSyncStatus = userSyncStatusService.getOrCreateForStack(stack);
                userSyncStatus.setUmsEventGenerationIds(new Json(umsEventGenerationIds));
                userSyncStatus.setLastFullSyncEndTime(Instant.now().toEpochMilli());
                userSyncStatusService.save(userSyncStatus);
            }
            return statusDetail;
        });

    }

    private SyncStatusDetail internalSynchronizeStack(Stack stack, UmsUsersState umsUsersState, boolean fullSync) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, fullSync);
            LOGGER.debug("IPA UsersState, found {} users and {} groups", ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

            applyStateDifferenceToIpa(stack.getEnvironmentCrn(), freeIpaClient,
                    UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState.getUsersState(), ipaUsersState));

            // Check for the password related attribute (cdpUserAttr) existence and go for password sync.
            processUsersWorkloadCredentials(environmentCrn, umsUsersState, freeIpaClient);

            return SyncStatusDetail.succeed(environmentCrn, "TODO- collect detail info");
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage());
        }
    }

    @VisibleForTesting
    UsersState getIpaUserState(FreeIpaClient freeIpaClient, UmsUsersState umsUsersState, boolean fullSync)
            throws FreeIpaClientException {
        return fullSync ? freeIpaUsersStateProvider.getUsersState(freeIpaClient) :
                freeIpaUsersStateProvider.getFilteredFreeIPAState(freeIpaClient, umsUsersState.getRequestedWorkloadUsers());
    }

    private void applyStateDifferenceToIpa(String environmentCrn, FreeIpaClient freeIpaClient, UsersStateDifference stateDifference)
            throws FreeIpaClientException {
        LOGGER.info("Applying state difference to environment {}.", environmentCrn);

        addGroups(freeIpaClient, stateDifference.getGroupsToAdd());
        addUsers(freeIpaClient, stateDifference.getUsersToAdd());
        addUsersToGroups(freeIpaClient, stateDifference.getGroupMembershipToAdd());

        removeUsersFromGroups(freeIpaClient, stateDifference.getGroupMembershipToRemove());
        removeUsers(freeIpaClient, stateDifference.getUsersToRemove());
    }

    private void processUsersWorkloadCredentials(
            String environmentCrn, UmsUsersState umsUsersState, FreeIpaClient freeIpaClient) throws IOException, FreeIpaClientException {
        Config config = freeIpaClient.getConfig();
        if (config.getIpauserobjectclasses() == null || !config.getIpauserobjectclasses().contains(Config.CDP_USER_ATTRIBUTE)) {
            LOGGER.debug("Doesn't seems like having config attribute, no credentials sync required for env:{}", environmentCrn);
            return;
        }

        // found the attribute, password sync can be performed
        LOGGER.debug("Having config attribute, going for credentials sync");

        // Should sync for all users and not just diff. At present there is no way to identify that there is a change in password for a user
        UsersState usersState = umsUsersState.getUsersState();
        for (FmsUser u : usersState.getUsers()) {
            WorkloadCredential workloadCredential = umsUsersState.getUsersWorkloadCredentialMap().get(u.getName());
            if (workloadCredential == null
                    || StringUtils.isEmpty(workloadCredential.getHashedPassword())
                    || CollectionUtils.isEmpty(workloadCredential.getKeys())) {
                continue;
            }

            // Call ASN_1 Encoder for encoding hashed password and then call user mod for password
            LOGGER.debug("Found Credentials for user {}", u.getName());
            String ansEncodedKrbPrincipalKey = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(workloadCredential.getKeys());

            freeIpaClient.userSetPasswordHash(u.getName(), workloadCredential.getHashedPassword(),
                    ansEncodedKrbPrincipalKey, workloadCredential.getExpirationDate());
            LOGGER.debug("Password synced for the user:{}, for the environment: {}", u.getName(), environmentCrn);
        }
    }

    private void addGroups(FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups) throws FreeIpaClientException {
        for (FmsGroup fmsGroup : fmsGroups) {
            LOGGER.debug("adding group {}", fmsGroup.getName());
            try {
                com.sequenceiq.freeipa.client.model.Group groupAdd = freeIpaClient.groupAdd(fmsGroup.getName());
                LOGGER.debug("Success: {}", groupAdd);
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add group {}", fmsGroup.getName(), e);
            }
        }
    }

    private void addUsers(FreeIpaClient freeIpaClient, Set<FmsUser> fmsUsers) throws FreeIpaClientException {
        for (FmsUser fmsUser : fmsUsers) {
            String username = fmsUser.getName();

            LOGGER.debug("adding user {}", username);

            try {
                com.sequenceiq.freeipa.client.model.User userAdd = freeIpaClient.userAdd(
                        username, fmsUser.getFirstName(), fmsUser.getLastName());
                LOGGER.debug("Success: {}", userAdd);
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add {}", username, e);
            }
        }
    }

    private void removeUsers(FreeIpaClient freeIpaClient, Set<String> fmsUsers) throws FreeIpaClientException {
        for (String username : fmsUsers) {
            LOGGER.debug("Removing user {}", username);
            try {
                com.sequenceiq.freeipa.client.model.User userRemove = freeIpaClient.deleteUser(username);
                LOGGER.debug("Success: {}", userRemove);
            } catch (FreeIpaClientException e) {
                LOGGER.error("Failed to delete {}", username, e);
            }
        }
    }

    private void removeGroups(FreeIpaClient freeIpaClient, Set<FmsGroup> fmsGroups) throws FreeIpaClientException {
        for (FmsGroup fmsGroup : fmsGroups) {
            String groupname = fmsGroup.getName();

            LOGGER.debug("Removing group {}", groupname);

            try {
                freeIpaClient.deleteGroup(groupname);
                LOGGER.debug("Success: {}", groupname);
            } catch (FreeIpaClientException e) {
                LOGGER.error("Failed to delete {}", groupname, e);
            }
        }

    }

    @VisibleForTesting
    void addUsersToGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        LOGGER.debug("adding users to groups: [{}]", groupMapping);
        for (String group : groupMapping.keySet()) {
            Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest).forEach(users -> {
                LOGGER.debug("adding users [{}] to group [{}]", users, group);
                try {
                    // TODO specialize response object
                    RPCResponse<Object> groupAddMember = freeIpaClient.groupAddMembers(group, users);
                    LOGGER.debug("Success: {}", groupAddMember.getResult());
                } catch (FreeIpaClientException e) {
                    // TODO propagate this information out to API
                    LOGGER.error("Failed to add [{}] to group [{}]", users, group, e);
                }
            });
        }
    }

    @VisibleForTesting
    void removeUsersFromGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        for (String group : groupMapping.keySet()) {
            Iterables.partition(groupMapping.get(group), maxSubjectsPerRequest).forEach(users -> {
                LOGGER.debug("removing users {} from group {}", users, group);
                try {
                    // TODO specialize response object
                    RPCResponse<Object> groupRemoveMembers = freeIpaClient.groupRemoveMembers(group, users);
                    LOGGER.debug("Success: {}", groupRemoveMembers.getResult());
                } catch (FreeIpaClientException e) {
                    // TODO propagate this information out to API
                    LOGGER.error("Failed to add [{}] to group [{}]", users, group, e);
                }
            });
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
