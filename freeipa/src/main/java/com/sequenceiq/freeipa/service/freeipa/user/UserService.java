package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.converter.freeipa.user.SyncOperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.user.model.UmsState;

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

    public SyncOperationStatus synchronizeUser(String accountId, String actorCrn, String userCrn) {
        return synchronizeAllUsers(accountId, actorCrn, null, Set.of(userCrn));
    }

    public SyncOperationStatus synchronizeAllUsers(String accountId, String actorCrn, Set<String> environmentFilter, Set<String> userCrnFilter) {
        SyncOperation response = syncOperationStatusService.startOperation(accountId, SyncOperationType.USER_SYNC);

        asyncTaskExecutor.submit(() -> asyncSynchronizeUsers(response.getOperationId(), accountId, actorCrn, environmentFilter, userCrnFilter));

        return syncOperationToSyncOperationStatus.convert(response);
    }

    public SyncStatusDetail syncAllUsersForStack(String accountId, String actorCrn, Stack stack) {
        UmsState umsState = umsUsersStateProvider.getUmsState(accountId, actorCrn);
        return synchronizeStack(stack, umsState, Set.of());
    }

    private void asyncSynchronizeUsers(String operationId, String accountId, String actorCrn, Set<String> environmentsFilter, Set<String> userCrnFilter) {
        try {
            boolean filterUsers = userCrnFilter != null && !userCrnFilter.isEmpty();

            // TODO allow filtering on machine users as well

            List<Stack> stacks = stackService.getAllByAccountId(accountId);
            LOGGER.debug("Found {} stacks for account {}", stacks.size(), accountId);
            if (environmentsFilter != null && !environmentsFilter.isEmpty()) {
                stacks = stacks.stream()
                        .filter(stack -> environmentsFilter.contains(stack.getEnvironmentCrn()))
                        .collect(Collectors.toList());
            }
            UmsState umsState = filterUsers ? umsUsersStateProvider.getUserFilteredUmsState(accountId, actorCrn, userCrnFilter)
                    : umsUsersStateProvider.getUmsState(accountId, actorCrn);

            Set<String> userIdFilter = filterUsers ? umsState.getUsernamesFromCrns(userCrnFilter)
                    : Set.of();

            Map<String, Future<SyncStatusDetail>> statusFutures = stacks.stream()
                    .collect(Collectors.toMap(Stack::getEnvironmentCrn,
                            stack -> asyncTaskExecutor.submit(() -> synchronizeStack(stack, umsState, userIdFilter))));


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
            syncOperationStatusService.failOperation(operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    private SyncStatusDetail synchronizeStack(Stack stack, UmsState umsState, Set<String> userIdFilter) {
        // TODO improve exception handling
        String environmentCrn = stack.getEnvironmentCrn();
        try {
            LOGGER.info("Syncing Environment {}", environmentCrn);
            UsersState umsUsersState = umsState.getUsersState(environmentCrn);
            LOGGER.debug("UMS UsersState = {}", umsUsersState);

            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            UsersState ipaUsersState = userIdFilter.isEmpty() ? freeIpaUsersStateProvider.getUsersState(freeIpaClient)
                    : freeIpaUsersStateProvider.getFilteredUsersState(freeIpaClient, userIdFilter);
            LOGGER.debug("IPA UsersState = {}", ipaUsersState);

            UsersStateDifference stateDifference = UsersStateDifference.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState);
            LOGGER.debug("State Difference = {}", stateDifference);

            addGroups(freeIpaClient, stateDifference.getGroupsToAdd());
            addUsers(freeIpaClient, stateDifference.getUsersToAdd());
            addUsersToGroups(freeIpaClient, stateDifference.getGroupMembershipToAdd());

            // TODO remove/deactivate groups/users/group membership

            return SyncStatusDetail.succeed(environmentCrn, "TODO- collect detail info");
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", stack.getEnvironmentCrn(), e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage());
        }
    }

    private void addGroups(FreeIpaClient freeIpaClient, Set<Group> groups) throws FreeIpaClientException {
        for (Group group : groups) {
            LOGGER.debug("adding group {}", group.getName());
            try {
                com.sequenceiq.freeipa.client.model.Group groupAdd = freeIpaClient.groupAdd(group.getName());
                LOGGER.debug("Success: {}", groupAdd);
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add group {}", group.getName(), e);
            }
        }
    }

    private void addUsers(FreeIpaClient freeIpaClient, Set<User> users) throws FreeIpaClientException {
        for (User user : users) {
            String username = user.getName();

            LOGGER.debug("adding user {}", username);

            try {
                com.sequenceiq.freeipa.client.model.User userAdd = freeIpaClient.userAdd(
                        username, user.getFirstName(), user.getLastName());
                LOGGER.debug("Success: {}", userAdd);
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add {}", username, e);
            }
        }
    }

    private void addUsersToGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        LOGGER.debug("adding users to groups: {}", groupMapping);
        for (String group : groupMapping.keySet()) {
            Set<String> users = Set.copyOf(groupMapping.get(group));
            LOGGER.debug("adding users {} to group {}", users, group);

            try {
                // TODO specialize response object
                RPCResponse<Object> groupAddMember = freeIpaClient.groupAddMembers(group, users);
                LOGGER.debug("Success: {}", groupAddMember.getResult());
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add {} to group", users, group, e);
            }
        }
    }

    private void removeUsersFromGroups(FreeIpaClient freeIpaClient, Multimap<String, String> groupMapping) throws FreeIpaClientException {
        for (String group : groupMapping.keySet()) {
            Set<String> users = Set.copyOf(groupMapping.get(group));
            LOGGER.debug("removing users {} to group {}", users, group);

            try {
                // TODO specialize response object
                RPCResponse<Object> groupRemoveMembers = freeIpaClient.groupRemoveMembers(group, users);
                LOGGER.debug("Success: {}", groupRemoveMembers.getResult());
            } catch (FreeIpaClientException e) {
                // TODO propagate this information out to API
                LOGGER.error("Failed to add {} to group", users, group, e);
            }
        }
    }
}