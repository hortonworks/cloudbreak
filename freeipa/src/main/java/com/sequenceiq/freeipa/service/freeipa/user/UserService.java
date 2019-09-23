package com.sequenceiq.freeipa.service.freeipa.user;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.user.SyncOperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.WorkloadCredentialsUtils;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private UmsUsersStateProvider umsUsersStateProvider;

    @Inject
    private FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Inject
    private AsyncTaskExecutor asyncTaskExecutor;

    @Inject
    private SyncOperationStatusService syncOperationStatusService;

    @Inject
    private SyncOperationToSyncOperationStatus syncOperationToSyncOperationStatus;

    public SyncOperationStatus synchronizeUsers(String accountId, String actorCrn, Set<String> environmentCrnFilter,
                                                Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {

        validateParameters(accountId, actorCrn, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);
        LOGGER.debug("Synchronizing users in account {} for environmentCrns {}, userCrns {}, and machineUserCrns {}",
                accountId, environmentCrnFilter, userCrnFilter, machineUserCrnFilter);

        List<Stack> stacks = getStacks(accountId, environmentCrnFilter);
        LOGGER.debug("Found {} stacks", stacks.size());
        if (stacks.isEmpty()) {
            throw new NotFoundException(String.format("No matching FreeIPA stacks found for account %s with environment crn filter %s",
                    accountId, environmentCrnFilter));
        }

        SyncOperation syncOperation = syncOperationStatusService
                .startOperation(accountId, SyncOperationType.USER_SYNC, environmentCrnFilter, union(userCrnFilter, machineUserCrnFilter));

        LOGGER.info("Starting operation [{}] with status [{}]", syncOperation.getOperationId(), syncOperation.getStatus());

        if (syncOperation.getStatus() == SynchronizationStatus.RUNNING) {
            MDCBuilder.addFlowId(syncOperation.getOperationId());
            asyncTaskExecutor.submit(() -> asyncSynchronizeUsers(
                syncOperation.getOperationId(), accountId, actorCrn, stacks, userCrnFilter, machineUserCrnFilter));
        }

        return syncOperationToSyncOperationStatus.convert(syncOperation);
    }

    private void asyncSynchronizeUsers(
        String operationId, String accountId, String actorCrn, List<Stack> stacks,
        Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        try {
            Set<String> environmentCrns = stacks.stream().map(Stack::getEnvironmentCrn).collect(Collectors.toSet());
            Map<String, UsersState> envToUmsStateMap = umsUsersStateProvider
                .getEnvToUmsUsersStateMap(accountId, actorCrn, environmentCrns, userCrnFilter, machineUserCrnFilter);

            Set<String> userIdFilter = Set.of();

            Map<String, Future<SyncStatusDetail>> statusFutures = stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                            stack -> asyncTaskExecutor.submit(() -> {
                                MDCBuilder.buildMdcContext(stack);
                                return synchronizeStack(stack, envToUmsStateMap.get(stack.getEnvironmentCrn()), userIdFilter);
                            })));

            List<SuccessDetails> success = new ArrayList<>();
            List<FailureDetails> failure = new ArrayList<>();

            statusFutures.forEach((envCrn, statusFuture) -> {
                SyncStatusDetail status;
                try {
                    status = statusFuture.get();
                    switch (status.getStatus()) {
                        case COMPLETED:
                            success.add(new SuccessDetails(envCrn));
                            break;
                        case FAILED:
                            failure.add(new FailureDetails(envCrn, status.getDetails()));
                            break;
                        default:
                            failure.add(new FailureDetails(envCrn, "Unknown status"));
                            break;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    failure.add(new FailureDetails(envCrn, e.getLocalizedMessage()));
                }
            });
            syncOperationStatusService.completeOperation(operationId, success, failure);
        } catch (RuntimeException e) {
            LOGGER.error("User sync operation {} failed with error:", operationId, e);
            syncOperationStatusService.failOperation(operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    private SyncStatusDetail synchronizeStack(Stack stack, UsersState umsUsersState, Set<String> userIdFilter) {

        // TODO improve exception handling
        // TODO: AP- Check if ums user has any user to be sync'ed, if not, skip. DH- revisit this check when implementing user removal
        String environmentCrn = stack.getEnvironmentCrn();
        try {
            LOGGER.info("Syncing Environment {}", environmentCrn);

            if (umsUsersState.getUsers() == null || umsUsersState.getUsers().size() == 0) {
                String message = "Failed to synchronize environment " + stack.getEnvironmentCrn() + " No User to sync for this environment";
                LOGGER.warn(message);
                return SyncStatusDetail.fail(environmentCrn, message);
            }

            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            UsersState ipaUsersState = userIdFilter.isEmpty() ? freeIpaUsersStateProvider.getUsersState(freeIpaClient)
                    : freeIpaUsersStateProvider.getFilteredUsersState(freeIpaClient, userIdFilter);
            LOGGER.debug("IPA UsersState, found {} users and {} groups", ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

            UsersStateDifference stateDifference = UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState);
            LOGGER.debug("State Difference = {}", stateDifference);

            addGroups(freeIpaClient, stateDifference.getGroupsToAdd());
            addUsers(freeIpaClient, stateDifference.getUsersToAdd());
            addUsersToGroups(freeIpaClient, stateDifference.getGroupMembershipToAdd());

            removeUsersFromGroups(freeIpaClient, stateDifference.getGroupMembershipToRemove());
            removeUsers(freeIpaClient, stateDifference.getUsersToRemove());
            removeGroups(freeIpaClient, stateDifference.getGroupsToRemove());

            // Check for the password related attribute (cdpUserAttr) existence and go for password sync.
            processUsersWorkloadCredentials(stack.getEnvironmentCrn(), umsUsersState, freeIpaClient);

            return SyncStatusDetail.succeed(environmentCrn, "TODO- collect detail info");
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", stack.getEnvironmentCrn(), e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage());
        }
    }

    private void processUsersWorkloadCredentials(String environmentCrn, UsersState umsUsersState, FreeIpaClient freeIpaClient) throws Exception {
        Config config = freeIpaClient.getConfig();
        if (config.getIpauserobjectclasses() != null && config.getIpauserobjectclasses().contains(Config.CDP_USER_ATTRIBUTE)) {
            // found the attribute, password sync can be performed
            LOGGER.debug("Going for Credentials Sync, seems new image FreeIPA Server, having config attribute");

            // Should sync for all users and not just diff. At present there is no way to identify that there is a change in password for a user
            for (FmsUser u : umsUsersState.getUsers()) {
                WorkloadCredential workloadCredential = umsUsersState.getUsersWorkloadCredentialMap().get(u.getName());
                if (workloadCredential != null
                    && !StringUtils.isEmpty(workloadCredential.getHashedPassword())
                    && CollectionUtils.isEmpty(workloadCredential.getKeys())) {

                    // Call ASN_1 Encoder for encoding hashed password and then call user mod for password
                    LOGGER.debug("Found Credentials for user {}", u.getName());
                    String ansEncodedKrbPrincipalKey = WorkloadCredentialsUtils.getEncodedKrbPrincipalKey(workloadCredential.getKeys());
                    Map<String, Object> params = new HashMap<>();
                    params.put("cdpHashedPassword", workloadCredential.getHashedPassword());
                    params.put("cdpUnencryptedKrbPrincipalKey", ansEncodedKrbPrincipalKey);
                    freeIpaClient.userMod(u.getName(), params);
                    LOGGER.debug("Password synced for the user:{}, for the environment: {}", u.getName(), environmentCrn);
                }
            }
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
                // TODO: Add method to delete group
                freeIpaClient.deleteGroup(groupname);
                LOGGER.debug("Success: {}", groupname);
            } catch (FreeIpaClientException e) {
                LOGGER.error("Failed to delete {}", groupname, e);
            }
        }

    }

    private void addUsersToGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        LOGGER.debug("adding users to groups: [{}]", groupMapping);
        for (String group : groupMapping.keySet()) {
            Set<String> users = Set.copyOf(groupMapping.get(group));
            LOGGER.debug("adding users [{}] to group [{}]", users, group);

            try {
                // TODO specialize response object
                RPCResponse<Object> groupAddMember = freeIpaClient.groupAddMembers(group, users);
                LOGGER.debug("Success: {}", groupAddMember.getResult());
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add [{}] to group [{}]", users, group, e);
            }
        }
    }

    private void removeUsersFromGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        for (String group : groupMapping.keySet()) {
            Set<String> users = Set.copyOf(groupMapping.get(group));
            LOGGER.debug("removing users {} from group {}", users, group);

            try {
                // TODO specialize response object
                RPCResponse<Object> groupRemoveMembers = freeIpaClient.groupRemoveMembers(group, users);
                LOGGER.debug("Success: {}", groupRemoveMembers.getResult());
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add [{}] to group [{}]", users, group, e);
            }
        }
    }

    private List<Stack> getStacks(String accountId, Set<String> environmentCrnFilter) {
        if (environmentCrnFilter.isEmpty()) {
            LOGGER.debug("Retrieving all stacks for account {}", accountId);
            return stackService.getAllByAccountId(accountId);
        } else {
            LOGGER.debug("Retrieving stacks for account {} that match environment crns {}", accountId, environmentCrnFilter);
            return stackService.getMultipleByEnvironmentCrnAndAccountId(environmentCrnFilter, accountId);
        }
    }

    @VisibleForTesting
    void validateParameters(String accountId, String actorCrn, Set<String> environmentCrnFilter,
            Set<String> userCrnFilter, Set<String> machineUserCrnFilter) {
        requireNonNull(accountId);
        requireNonNull(actorCrn);
        requireNonNull(environmentCrnFilter);
        requireNonNull(userCrnFilter);
        requireNonNull(machineUserCrnFilter);
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

    protected Set<String> union(Collection<String> collection1, Collection<String> collection2) {
        return Stream.concat(collection1.stream(), collection2.stream()).collect(Collectors.toSet());
    }
}
