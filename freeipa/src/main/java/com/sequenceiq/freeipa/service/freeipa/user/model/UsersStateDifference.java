package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.client.FreeIpaChecks;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;

public class UsersStateDifference {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersStateDifference.class);

    private ImmutableSet<FmsGroup> groupsToAdd;

    private ImmutableSet<FmsGroup> groupsToRemove;

    private ImmutableSet<FmsUser> usersToAdd;

    private final ImmutableSet<String> usersWithCredentialsToUpdate;

    private ImmutableSet<String> usersToRemove;

    private ImmutableMultimap<String, String> groupMembershipToAdd;

    private ImmutableMultimap<String, String> groupMembershipToRemove;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public UsersStateDifference(ImmutableSet<FmsGroup> groupsToAdd, ImmutableSet<FmsGroup> groupsToRemove,
            ImmutableSet<FmsUser> usersToAdd, ImmutableSet<String> usersWithCredentialsToUpdate, ImmutableSet<String> usersToRemove,
            ImmutableMultimap<String, String> groupMembershipToAdd, ImmutableMultimap<String, String> groupMembershipToRemove) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToAdd = requireNonNull(usersToAdd);
        this.usersWithCredentialsToUpdate = requireNonNull(usersWithCredentialsToUpdate);
        this.usersToRemove = requireNonNull(usersToRemove);
        this.groupMembershipToAdd = requireNonNull(groupMembershipToAdd);
        this.groupMembershipToRemove = requireNonNull(groupMembershipToRemove);
    }

    public ImmutableSet<FmsGroup> getGroupsToAdd() {
        return groupsToAdd;
    }

    public ImmutableSet<FmsGroup> getGroupsToRemove() {
        return groupsToRemove;
    }

    public ImmutableSet<FmsUser> getUsersToAdd() {
        return usersToAdd;
    }

    public ImmutableSet<String> getUsersWithCredentialsToUpdate() {
        return usersWithCredentialsToUpdate;
    }

    public ImmutableSet<String> getUsersToRemove() {
        return usersToRemove;
    }

    public ImmutableMultimap<String, String> getGroupMembershipToAdd() {
        return groupMembershipToAdd;
    }

    public ImmutableMultimap<String, String> getGroupMembershipToRemove() {
        return groupMembershipToRemove;
    }

    @Override
    public String toString() {
        return "UsersStateDifference{"
                + "groupsToAdd=" + groupsToAdd
                + ", groupsToRemove=" + groupsToRemove
                + ", usersToAdd=" + usersToAdd
                + ", usersWithCredentialsToUpdate=" + usersWithCredentialsToUpdate
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + '}';
    }

    public static UsersStateDifference fromUmsAndIpaUsersStates(UmsUsersState umsState, UsersState ipaState,
            boolean credentialsUpdateOptimizationEnabled) {
        return new UsersStateDifference(
                calculateGroupsToAdd(umsState, ipaState),
                calculateGroupsToRemove(umsState, ipaState),
                calculateUsersToAdd(umsState, ipaState),
                calculateUsersWithCredentialsToUpdate(umsState, ipaState, credentialsUpdateOptimizationEnabled),
                calculateUsersToRemove(umsState, ipaState),
                calculateGroupMembershipToAdd(umsState, ipaState),
                calculateGroupMembershipToRemove(umsState, ipaState));
    }

    public static UsersStateDifference forDeletedUser(String deletedUser, Collection<String> groupMembershipsToRemove) {
        Multimap<String, String> groupMembershipsToRemoveMap = HashMultimap.create();
        groupMembershipsToRemoveMap.putAll(deletedUser, groupMembershipsToRemove);
        return new UsersStateDifference(
                ImmutableSet.of(),
                ImmutableSet.of(),
                ImmutableSet.of(),
                ImmutableSet.of(),
                ImmutableSet.of(deletedUser),
                ImmutableMultimap.of(),
                ImmutableMultimap.copyOf(groupMembershipsToRemoveMap));
    }

    public static ImmutableSet<FmsUser> calculateUsersToAdd(UmsUsersState umsState, UsersState ipaState) {
        ImmutableSet<FmsUser> usersToAdd = ImmutableSet.copyOf(Sets.difference(umsState.getUsersState().getUsers(), ipaState.getUsers())
                .stream()
                .filter(fmsUser -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(fmsUser.getName()))
                .collect(Collectors.toSet()));

        LOGGER.info("usersToAdd size = {}", usersToAdd.size());
        LOGGER.debug("userToAdd = {}", usersToAdd.stream().map(FmsUser::getName).collect(Collectors.toSet()));

        return usersToAdd;
    }

    public static ImmutableSet<String> calculateUsersWithCredentialsToUpdate(UmsUsersState umsState, UsersState ipaState,
            boolean credentialsUpdateOptimizationEnabled) {
        ImmutableSet<String> usersWithCredentialsToUpdate;

        if (!credentialsUpdateOptimizationEnabled) {
            usersWithCredentialsToUpdate = ImmutableSet.copyOf(umsState.getUsersState().getUsers().stream()
                    .map(FmsUser::getName)
                    .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username))
                    .collect(Collectors.toSet()));
        } else {
            ImmutableSet.Builder<String> usersWithCredentialsToUpdateBuilder = ImmutableSet.builder();
            umsState.getUsersState().getUsers().stream()
                    .map(FmsUser::getName)
                    .filter(username -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(username))
                    .forEach(username -> {
                        UserMetadata ipaUserMetadata = ipaState.getUserMetadataMap().get(username);
                        if (ipaUserMetadata == null) {
                            usersWithCredentialsToUpdateBuilder.add(username);
                        } else {
                            WorkloadCredential umsCredential = umsState.getUsersWorkloadCredentialMap().get(username);
                            if (ipaUserMetadata.getWorkloadCredentialsVersion() < umsCredential.getVersion()) {
                                usersWithCredentialsToUpdateBuilder.add(username);
                            }
                        }
                    });
            usersWithCredentialsToUpdate = usersWithCredentialsToUpdateBuilder.build();
        }

        LOGGER.info("usersWithCredentialsToUpdate size = {}", usersWithCredentialsToUpdate.size());
        LOGGER.debug("usersWithCredentialsToUpdate = {}", usersWithCredentialsToUpdate);
        return usersWithCredentialsToUpdate;
    }

    public static ImmutableSet<String> calculateUsersToRemove(UmsUsersState umsState, UsersState ipaState) {
        Collection<String> umsStateUsers = umsState.getUsersState().getGroupMembership().get(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP);
        Collection<String> ipaStateUsers = ipaState.getGroupMembership().get(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP);

        ImmutableSet<String> usersToRemove = ImmutableSet.copyOf(ipaStateUsers.stream()
                .filter(ipaUser -> !umsStateUsers.contains(ipaUser))
                .filter(ipaUser -> !FreeIpaChecks.IPA_PROTECTED_USERS.contains(ipaUser))
                .collect(Collectors.toSet()));

        LOGGER.info("usersToRemove size = {}", usersToRemove.size());
        LOGGER.debug("usersToRemove = {}", usersToRemove);

        return usersToRemove;
    }

    public static ImmutableSet<FmsGroup> calculateGroupsToAdd(UmsUsersState umsState, UsersState ipaState) {
        ImmutableSet<FmsGroup> groupsToAdd = ImmutableSet.copyOf(Sets.difference(umsState.getUsersState().getGroups(), ipaState.getGroups())
                .stream()
                .filter(fmsGroup -> !FreeIpaChecks.IPA_PROTECTED_GROUPS.contains(fmsGroup.getName()))
                .collect(Collectors.toSet()));

        LOGGER.info("groupsToAdd size = {}", groupsToAdd.size());
        LOGGER.debug("groupsToAdd = {}", groupsToAdd.stream().map(FmsGroup::getName).collect(Collectors.toSet()));

        return groupsToAdd;
    }

    public static ImmutableSet<FmsGroup> calculateGroupsToRemove(UmsUsersState umsState, UsersState ipaState) {
        Set<FmsGroup> allControlPlaneGroups = Sets.union(umsState.getWorkloadAdministrationGroups(), umsState.getUsersState().getGroups());

        ImmutableSet<FmsGroup> groupsToRemove = ImmutableSet.copyOf(Sets.difference(ipaState.getGroups(), allControlPlaneGroups)
                .stream()
                .filter(fmsGroup -> !FreeIpaChecks.IPA_PROTECTED_GROUPS.contains(fmsGroup.getName()))
                .collect(Collectors.toSet()));

        LOGGER.info("groupsToRemove size = {}", groupsToRemove.size());
        LOGGER.debug("groupsToRemove = {}", groupsToRemove.stream().map(FmsGroup::getName).collect(Collectors.toSet()));

        return groupsToRemove;
    }

    public static ImmutableMultimap<String, String> calculateGroupMembershipToAdd(UmsUsersState umsState, UsersState ipaState) {
        Multimap<String, String> groupMembershipToAdd = HashMultimap.create();
        umsState.getUsersState().getGroupMembership().forEach((group, user) -> {
            if (!FreeIpaChecks.IPA_UNMANAGED_GROUPS.contains(group) && !ipaState.getGroupMembership().containsEntry(group, user)) {
                LOGGER.debug("adding user : {} to group : {}", user, group);
                groupMembershipToAdd.put(group, user);
            }
        });

        LOGGER.info("groupMembershipToAdd size = {}", groupMembershipToAdd.size());
        LOGGER.debug("groupMembershipToAdd = {}", groupMembershipToAdd.asMap());

        return ImmutableMultimap.copyOf(groupMembershipToAdd);
    }

    public static ImmutableMultimap<String, String> calculateGroupMembershipToRemove(UmsUsersState umsState, UsersState ipaState) {
        Multimap<String, String> groupMembershipToRemove = HashMultimap.create();
        ipaState.getGroupMembership().forEach((group, user) -> {
            if (!FreeIpaChecks.IPA_UNMANAGED_GROUPS.contains(group) && !umsState.getUsersState().getGroupMembership().containsEntry(group, user)) {
                LOGGER.debug("removing user : {} to group : {}", user, group);
                groupMembershipToRemove.put(group, user);
            }
        });

        LOGGER.info("groupMembershipToRemove size = {}", groupMembershipToRemove.size());
        LOGGER.debug("groupMembershipToRemove = {}", groupMembershipToRemove.asMap());

        return ImmutableMultimap.copyOf(groupMembershipToRemove);
    }
}
