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

    private ImmutableSet<String> usersToRemove;

    private ImmutableMultimap<String, String> groupMembershipToAdd;

    private ImmutableMultimap<String, String> groupMembershipToRemove;

    public UsersStateDifference(ImmutableSet<FmsGroup> groupsToAdd, ImmutableSet<FmsGroup> groupsToRemove,
            ImmutableSet<FmsUser> usersToAdd, ImmutableSet<String> usersToRemove,
            ImmutableMultimap<String, String> groupMembershipToAdd, ImmutableMultimap<String, String> groupMembershipToRemove) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToAdd = requireNonNull(usersToAdd);
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
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + '}';
    }

    public static UsersStateDifference fromUmsAndIpaUsersStates(UmsUsersState umsState, UsersState ipaState) {
        return new UsersStateDifference(
                calculateGroupsToAdd(umsState, ipaState),
                calculateGroupsToRemove(umsState, ipaState),
                calculateUsersToAdd(umsState, ipaState),
                calculateUsersToRemove(umsState, ipaState),
                calculateGroupMembershipToAdd(umsState, ipaState),
                calculateGroupMembershipToRemove(umsState, ipaState));
    }

    public static UsersStateDifference forDeletedUser(String deletedUser, Collection<String> groupsToRemove) {
        Multimap<String, String> groupMembershipsToRemove = HashMultimap.create();
        groupMembershipsToRemove.putAll(deletedUser, groupsToRemove);
        return new UsersStateDifference(
                ImmutableSet.of(),
                ImmutableSet.of(),
                ImmutableSet.of(),
                ImmutableSet.of(deletedUser),
                ImmutableMultimap.of(),
                ImmutableMultimap.copyOf(groupMembershipsToRemove));
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
        Set<FmsGroup> allGroups = Sets.union(umsState.getWorkloadAdministrationGroups(), umsState.getUsersState().getGroups());

        ImmutableSet<FmsGroup> groupsToRemove = ImmutableSet.copyOf(Sets.difference(ipaState.getGroups(), allGroups)
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
