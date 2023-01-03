package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Component
public class UserStateDifferenceCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserStateDifferenceCalculator.class);

    private static final String GROUP_SIZE_LIMIT_EXCEEDED_ERROR = "Group size limit exceeded";

    private static final String GROUP_SIZE_LIMIT_EXCEEDED_MESSAGE =
            "%s contains %d users. This exceeds maximum group size limit of %d. New members will not be added to this group.";

    public UsersStateDifference fromUmsAndIpaUsersStates(UmsUsersState umsState, UsersState ipaState,
            UserSyncOptions options, BiConsumer<String, String> warnings) {
        return new UsersStateDifference(
                calculateGroupsToAdd(umsState, ipaState),
                calculateGroupsToRemove(umsState, ipaState),
                calculateUsersToAdd(umsState, ipaState),
                calculateUsersWithCredentialsToUpdate(umsState, ipaState, options.isCredentialsUpdateOptimizationEnabled()),
                calculateUsersToRemove(umsState, ipaState),
                calculateGroupMembershipToAdd(umsState, ipaState, options, warnings),
                calculateGroupMembershipToRemove(umsState, ipaState),
                calculateUsersToDisable(umsState, ipaState),
                calculateUsersToEnable(umsState, ipaState));
    }

    public ImmutableSet<FmsGroup> calculateGroupsToAdd(UmsUsersState umsState, UsersState ipaState) {
        ImmutableSet<FmsGroup> groupsToAdd = ImmutableSet.copyOf(Sets.difference(umsState.getUsersState().getGroups(), ipaState.getGroups())
                .stream()
                .filter(fmsGroup -> !FreeIpaChecks.IPA_PROTECTED_GROUPS.contains(fmsGroup.getName()))
                .collect(Collectors.toSet()));

        LOGGER.info("groupsToAdd size = {}", groupsToAdd.size());
        LOGGER.debug("groupsToAdd = {}", groupsToAdd.stream().map(FmsGroup::getName).collect(Collectors.toSet()));

        return groupsToAdd;
    }

    public ImmutableSet<FmsGroup> calculateGroupsToRemove(UmsUsersState umsState, UsersState ipaState) {
        Set<FmsGroup> allControlPlaneGroups = Sets.union(umsState.getWorkloadAdministrationGroups(), umsState.getUsersState().getGroups());

        ImmutableSet<FmsGroup> groupsToRemove = ImmutableSet.copyOf(Sets.difference(ipaState.getGroups(), allControlPlaneGroups)
                .stream()
                .filter(fmsGroup -> !FreeIpaChecks.IPA_PROTECTED_GROUPS.contains(fmsGroup.getName()))
                .collect(Collectors.toSet()));

        LOGGER.info("groupsToRemove size = {}", groupsToRemove.size());
        LOGGER.debug("groupsToRemove = {}", groupsToRemove.stream().map(FmsGroup::getName).collect(Collectors.toSet()));

        return groupsToRemove;
    }

    public ImmutableSet<FmsUser> calculateUsersToAdd(UmsUsersState umsState, UsersState ipaState) {
        Map<String, FmsUser> umsUsers = umsState.getUsersState().getUsers().stream()
                .collect(Collectors.toMap(FmsUser::getName, Function.identity(), (r1, r2) -> r1));
        Set<String> ipaUsers = ipaState.getUsers().stream()
                .map(FmsUser::getName)
                .collect(Collectors.toSet());
        ImmutableSet<FmsUser> usersToAdd = ImmutableSet.copyOf(Sets.difference(umsUsers.keySet(), ipaUsers)
                .stream()
                .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username))
                .map(umsUsers::get)
                .collect(Collectors.toSet()));

        LOGGER.info("usersToAdd size = {}", usersToAdd.size());
        LOGGER.debug("userToAdd = {}", usersToAdd.stream().map(FmsUser::getName).collect(Collectors.toSet()));

        return usersToAdd;
    }

    public ImmutableSet<String> calculateUsersWithCredentialsToUpdate(UmsUsersState umsState, UsersState ipaState,
            boolean credentialsUpdateOptimizationEnabled) {
        ImmutableSet<String> usersWithCredentialsToUpdate = credentialsUpdateOptimizationEnabled ?
                getUsersWithStaleCredentials(umsState, ipaState) : getAllUsers(umsState);

        LOGGER.info("usersWithCredentialsToUpdate size = {}", usersWithCredentialsToUpdate.size());
        LOGGER.debug("usersWithCredentialsToUpdate = {}", usersWithCredentialsToUpdate);
        return usersWithCredentialsToUpdate;
    }

    public ImmutableSet<String> calculateUsersToRemove(UmsUsersState umsState, UsersState ipaState) {
        Collection<String> umsStateUsers = umsState.getUsersState().getGroupMembership().get(CDP_USERSYNC_INTERNAL_GROUP);
        Collection<String> ipaStateUsers = ipaState.getGroupMembership().get(CDP_USERSYNC_INTERNAL_GROUP);

        ImmutableSet<String> usersToRemove = ImmutableSet.copyOf(ipaStateUsers.stream()
                .filter(ipaUser -> !umsStateUsers.contains(ipaUser))
                .filter(ipaUser -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(ipaUser))
                .collect(Collectors.toSet()));

        LOGGER.info("usersToRemove size = {}", usersToRemove.size());
        LOGGER.debug("usersToRemove = {}", usersToRemove);

        return usersToRemove;
    }

    public ImmutableMultimap<String, String> calculateGroupMembershipToAdd(UmsUsersState umsState, UsersState ipaState,
            UserSyncOptions options, BiConsumer<String, String> warnings) {
        Multimap<String, String> groupMembershipToAdd = HashMultimap.create();
        Set<String> groupsExceedingLimit = umsState.getGroupsExceedingLimit().stream()
                .filter(Predicate.not(UserSyncConstants.ALLOWED_LARGE_GROUP_PREDICATE))
                .collect(Collectors.toSet());
        boolean enforceGroupLimits = options.isEnforceGroupMembershipLimitEnabled();
        int groupLimit = options.getLargeGroupLimit();

        umsState.getUsersState().getGroupMembership().asMap().forEach((group, users) -> {
            if (!FreeIpaChecks.IPA_UNMANAGED_GROUPS.contains(group)) {
                if (enforceGroupLimits && groupsExceedingLimit.contains(group)) {
                    String message = String.format(GROUP_SIZE_LIMIT_EXCEEDED_MESSAGE, group, users.size(), groupLimit);
                    LOGGER.debug(message);
                    warnings.accept(GROUP_SIZE_LIMIT_EXCEEDED_ERROR, message);
                } else {
                    Set<String> usersToAdd = users.stream()
                            .filter(user -> !ipaState.getGroupMembership().containsEntry(group, user))
                            .collect(Collectors.toSet());
                    LOGGER.debug("adding users : {} to group : {}", usersToAdd, group);
                    groupMembershipToAdd.putAll(group, usersToAdd);
                }
            }
        });

        LOGGER.info("groupMembershipToAdd size = {}", groupMembershipToAdd.size());
        LOGGER.debug("groupMembershipToAdd = {}", groupMembershipToAdd.asMap());

        return ImmutableMultimap.copyOf(groupMembershipToAdd);
    }

    public ImmutableMultimap<String, String> calculateGroupMembershipToRemove(UmsUsersState umsState, UsersState ipaState) {
        Multimap<String, String> groupMembershipToRemove = HashMultimap.create();
        ipaState.getGroupMembership().forEach((group, user) -> {
            if (!FreeIpaChecks.IPA_UNMANAGED_GROUPS.contains(group) &&
                    ipaState.getGroupMembership().containsEntry(CDP_USERSYNC_INTERNAL_GROUP, user) &&
                    !umsState.getUsersState().getGroupMembership().containsEntry(group, user)) {
                LOGGER.debug("removing user : {} to group : {}", user, group);
                groupMembershipToRemove.put(group, user);
            }
        });

        LOGGER.info("groupMembershipToRemove size = {}", groupMembershipToRemove.size());
        LOGGER.debug("groupMembershipToRemove = {}", groupMembershipToRemove.asMap());

        return ImmutableMultimap.copyOf(groupMembershipToRemove);
    }

    public ImmutableSet<String> calculateUsersToDisable(UmsUsersState umsState, UsersState ipaState) {
        ImmutableSet<String> usersToDisable = calculateUsersWithDifferingState(umsState, ipaState, FmsUser.State.DISABLED);
        LOGGER.info("usersToDisable size = {}", usersToDisable.size());
        LOGGER.debug("userToDisable = {}", usersToDisable);

        return usersToDisable;
    }

    public ImmutableSet<String> calculateUsersToEnable(UmsUsersState umsState, UsersState ipaState) {
        ImmutableSet<String> usersToEnable = calculateUsersWithDifferingState(umsState, ipaState, FmsUser.State.ENABLED);
        LOGGER.info("usersToEnable size = {}", usersToEnable.size());
        LOGGER.debug("userToEnable = {}", usersToEnable);

        return usersToEnable;
    }

    public boolean usersStateDifferenceChanged(UsersStateDifference beforeSync, UsersStateDifference afterSync) {
        return beforeSync.getUsersToAdd().size() != afterSync.getUsersToAdd().size() ||
                beforeSync.getUsersToRemove().size() != afterSync.getUsersToRemove().size() ||
                beforeSync.getGroupsToAdd().size() != afterSync.getUsersToAdd().size() ||
                beforeSync.getGroupsToRemove().size() != afterSync.getGroupsToRemove().size() ||
                beforeSync.getGroupMembershipToAdd().size() != afterSync.getGroupMembershipToAdd().size() ||
                beforeSync.getGroupMembershipToRemove().size() != afterSync.getGroupMembershipToRemove().size() ||
                beforeSync.getUsersWithCredentialsToUpdate().size() != afterSync.getUsersWithCredentialsToUpdate().size();
    }

    private ImmutableSet<String> getUsersWithStaleCredentials(UmsUsersState umsState, UsersState ipaState) {
        return umsState.getUsersState().getUsers().stream()
                .map(FmsUser::getName)
                .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username) && credentialsAreStale(username, umsState, ipaState))
                .collect(ImmutableSet.toImmutableSet());
    }

    private boolean credentialsAreStale(String username, UmsUsersState umsState, UsersState ipaState) {
        UserMetadata ipaUserMetadata = ipaState.getUserMetadataMap().get(username);
        if (ipaUserMetadata != null) {
            WorkloadCredential umsCredential = umsState.getUsersWorkloadCredentialMap().get(username);
            return ipaUserMetadata.getWorkloadCredentialsVersion() < umsCredential.getVersion();
        }
        return true;
    }

    private static ImmutableSet<String> getAllUsers(UmsUsersState umsState) {
        return umsState.getUsersState().getUsers().stream()
                .map(FmsUser::getName)
                .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<String> calculateUsersWithDifferingState(
            UmsUsersState umsState, UsersState ipaState, FmsUser.State state) {
        Map<String, FmsUser> existingIpaUsers = ipaState.getUsers().stream()
                .collect(Collectors.toMap(FmsUser::getName, Function.identity()));
        return ImmutableSet.copyOf(umsState.getUsersState().getUsers().stream()
                .filter(u -> u.getState() == state &&
                        existingIpaUsers.containsKey(u.getName()) &&
                        existingIpaUsers.get(u.getName()).getState() != state)
                .map(FmsUser::getName)
                .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username))
                .collect(Collectors.toSet()));
    }
}
