package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_GROUPS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_USERS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_USERS_TO_GROUPS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.APPLY_DIFFERENCE_TO_IPA;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.DISABLE_USERS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ENABLE_USERS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.REMOVE_GROUPS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.REMOVE_USERS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.REMOVE_USERS_FROM_GROUPS;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.SET_WORKLOAD_CREDENTIALS;
import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.client.FreeIpaCapabilities;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;

@Component
public class UserSyncStateApplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncStateApplier.class);

    @Inject
    private WorkloadCredentialService workloadCredentialService;

    @Inject
    private UserSyncOperations operations;

    @Inject
    private UserSyncGroupAddMemberOperations groupAddMemberOperations;

    public void applyDifference(UmsUsersState umsUsersState, String environmentCrn, Multimap<String, String> warnings,
            UsersStateDifference usersStateDifference, UserSyncOptions options, FreeIpaClient freeIpaClient,
            Long stackId) throws FreeIpaClientException, TimeoutException {
        LOGGER.debug("Starting {} ...", APPLY_DIFFERENCE_TO_IPA);
        applyStateDifferenceToIpa(umsUsersState, environmentCrn, freeIpaClient, usersStateDifference, warnings::put, options.isFmsToFreeIpaBatchCallEnabled());
        LOGGER.debug("Finished {}.", APPLY_DIFFERENCE_TO_IPA);

        if (!FreeIpaCapabilities.hasSetPasswordHashSupport(freeIpaClient.getConfig())) {
            LOGGER.debug("IPA doesn't have password hash support, no credentials sync required for env:{}", environmentCrn);
        } else {
            LOGGER.debug("Starting {} for {} users ...", SET_WORKLOAD_CREDENTIALS, usersStateDifference.getUsersWithCredentialsToUpdate().size());
            ImmutableSet<WorkloadCredentialUpdate> credentialUpdates = usersStateDifference.getUsersWithCredentialsToUpdate().stream()
                    .map(username -> getCredentialUpdate(username, umsUsersState))
                    .collect(ImmutableSet.toImmutableSet());
            workloadCredentialService.setWorkloadCredentials(options, freeIpaClient, credentialUpdates, warnings::put, stackId);
            LOGGER.debug("Finished {}.", SET_WORKLOAD_CREDENTIALS);
        }
    }

    public void applyStateDifferenceToIpa(UmsUsersState umsUsersState, String environmentCrn, FreeIpaClient freeIpaClient, UsersStateDifference stateDifference,
            BiConsumer<String, String> warnings, boolean fmsToFreeipaBatchCallEnabled) throws FreeIpaClientException, TimeoutException {
        LOGGER.info("Applying state difference {} to environment {}.", stateDifference, environmentCrn);

        LOGGER.debug("Starting {} for {} groups ...", ADD_GROUPS,
                stateDifference.getGroupsToAdd().size());
        operations.addGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupsToAdd(), warnings);
        LOGGER.debug("Finished {}.", ADD_GROUPS);

        LOGGER.debug("Starting {} for {} users ...", ADD_USERS,
                stateDifference.getUsersToAdd().size());
        operations.addUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToAdd(), warnings);
        LOGGER.debug("Finished {}.", ADD_USERS);

        LOGGER.debug("Starting {} for {} users ...", DISABLE_USERS,
                stateDifference.getUsersToDisable().size());
        operations.disableUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToDisable(), warnings);
        LOGGER.debug("Finished {}.", DISABLE_USERS);

        LOGGER.debug("Starting {} for {} users ...", ENABLE_USERS,
                stateDifference.getUsersToEnable().size());
        operations.enableUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToEnable(), warnings);
        LOGGER.debug("Finished {}.", ENABLE_USERS);

        LOGGER.debug("Starting {} for {} group memberships ...", ADD_USERS_TO_GROUPS,
                stateDifference.getGroupMembershipToAdd().size());
        addGroupMemberships(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupMembershipToAdd(),
                umsUsersState.getGroupsExceedingThreshold(), warnings);
        LOGGER.debug("Finished {}.", ADD_USERS_TO_GROUPS);

        LOGGER.debug("Starting {} for {} group memberships ...", REMOVE_USERS_FROM_GROUPS,
                stateDifference.getGroupMembershipToRemove().size());
        operations.removeUsersFromGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupMembershipToRemove(), warnings);
        LOGGER.debug("Finished {}.", REMOVE_USERS_FROM_GROUPS);

        LOGGER.debug("Starting {} for {} users ...", REMOVE_USERS,
                stateDifference.getUsersToRemove().size());
        operations.removeUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getUsersToRemove(), warnings);
        LOGGER.debug("Finished {}.", REMOVE_USERS);

        LOGGER.debug("Starting {} for {} groups ...", REMOVE_GROUPS,
                stateDifference.getGroupsToRemove().size());
        operations.removeGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, stateDifference.getGroupsToRemove(), warnings);
        LOGGER.debug("Finished {}.", REMOVE_GROUPS);
    }

    private void addGroupMemberships(boolean fmsToFreeipaBatchCallEnabled, FreeIpaClient freeIpaClient, Multimap<String, String> groupMembershipToAdd,
            Set<String> largeGroupNames, BiConsumer<String, String> warnings) throws FreeIpaClientException, TimeoutException {
        groupAddMemberOperations.addMembersToSmallGroups(fmsToFreeipaBatchCallEnabled, freeIpaClient, groupMembershipToAdd, largeGroupNames, warnings);
        groupAddMemberOperations.addMembersToLargeGroups(freeIpaClient, groupMembershipToAdd, largeGroupNames, warnings);
    }

    public void applyUserDeleteToIpa(String environmentCrn, FreeIpaClient freeIpaClient, String userToDelete,
            BiConsumer<String, String> warnings, boolean fmsToFreeipaBatchCallEnabled) throws FreeIpaClientException, TimeoutException {
        LOGGER.info("Applying user delete {} to environment {}.", userToDelete, environmentCrn);

        LOGGER.debug("Starting {} for {} users ...", REMOVE_USERS, 1);
        operations.removeUsers(fmsToFreeipaBatchCallEnabled, freeIpaClient, ImmutableSet.of(userToDelete), warnings);
        LOGGER.debug("Finished {}.", REMOVE_USERS);
    }

    private WorkloadCredentialUpdate getCredentialUpdate(String username, UmsUsersState umsUsersState) {
        UserMetadata userMetadata = requireNonNull(umsUsersState.getUsersState().getUserMetadataMap().get(username),
                "userMetadata must not be null");
        WorkloadCredential workloadCredential = requireNonNull(umsUsersState.getUsersWorkloadCredentialMap().get(username),
                "workloadCredential must not be null");
        return new WorkloadCredentialUpdate(username, userMetadata.getCrn(), workloadCredential);
    }
}
