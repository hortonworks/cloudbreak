package com.sequenceiq.freeipa.service.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.freeipa.service.freeipa.user.UserServiceConstants;

public class UsersStateDifference {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersStateDifference.class);

    private Set<FmsGroup> groupsToAdd;

    private Set<FmsUser> usersToAdd;

    private Set<FmsGroup> groupsToRemove;

    private Set<FmsUser> usersToRemove;

    private Multimap<String, String> groupMembershipToAdd;

    private Multimap<String, String> groupMembershipToRemove;

    public UsersStateDifference(Set<FmsGroup> groupsToAdd, Set<FmsUser> usersToAdd, Set<FmsGroup> groupsToRemove, Set<FmsUser> usersToRemove,
            Multimap<String, String> groupMembershipToAdd, Multimap<String, String> groupMembershipToRemove) {
        this.groupsToAdd = requireNonNull(groupsToAdd);
        this.usersToAdd = requireNonNull(usersToAdd);
        this.groupsToRemove = requireNonNull(groupsToRemove);
        this.usersToRemove = requireNonNull(usersToRemove);
        this.groupMembershipToAdd = requireNonNull(groupMembershipToAdd);
        this.groupMembershipToRemove = requireNonNull(groupMembershipToRemove);
    }

    public Set<FmsGroup> getGroupsToAdd() {
        return groupsToAdd;
    }

    public Set<FmsUser> getUsersToAdd() {
        return usersToAdd;
    }

    public Set<FmsGroup> getGroupsToRemove() {
        return groupsToRemove;
    }

    public Set<FmsUser> getUsersToRemove() {
        return usersToRemove;
    }

    public Multimap<String, String> getGroupMembershipToAdd() {
        return groupMembershipToAdd;
    }

    public Multimap<String, String> getGroupMembershipToRemove() {
        return groupMembershipToRemove;
    }

    @Override
    public String toString() {
        return "UsersStateDifference{"
                + "groupsToAdd=" + groupsToAdd
                + ", usersToAdd=" + usersToAdd
                + ", groupsToRemove=" + groupsToRemove
                + ", usersToRemove=" + usersToRemove
                + ", groupMembershipToAdd=" + groupMembershipToAdd
                + ", groupMembershipToRemove=" + groupMembershipToRemove
                + '}';
    }

    public static UsersStateDifference fromUmsAndIpaUsersStates(UsersState umsState, UsersState ipaState) {
        Multimap<String, String> umsGroupMembership = umsState.getGroupMembership();
        Multimap<String, String> ipaGroupMembership = ipaState.getGroupMembership();

        Multimap<String, String> groupMembershipToAdd = HashMultimap.create();
        umsGroupMembership.forEach((group, user) -> {
            if (!ipaGroupMembership.containsEntry(group, user)) {
                LOGGER.debug("adding user : {} to group : {}", user, group);
                groupMembershipToAdd.put(group, user);
            }
        });
        LOGGER.info("groupMembershipToAdd size= {}", groupMembershipToAdd.size());

        Multimap<String, String> groupMembershipToRemove = HashMultimap.create();
        ipaGroupMembership.forEach((group, user) -> {
            if (!umsGroupMembership.containsEntry(group, user)) {
                LOGGER.debug("removing user : {} to group : {}", user, group);
                groupMembershipToRemove.put(group, user);
            }
        });

        LOGGER.info("groupMembershipToRemove size= {}", groupMembershipToRemove.size());

        Set<FmsUser> usersToRemove =
            getUsersToBeRemoved(umsState.getGroupMembership().get(UserServiceConstants.USERSYNC_INTERNAL_GROUP),
                                ipaState.getUsers());
        LOGGER.info("usersToRemove size= {}", usersToRemove.size());

        Set<FmsGroup> groupsToBeRemoved = getGroupsToBeRemoved(umsState.getGroups(), ipaState.getGroups());
        return new UsersStateDifference(
            Set.copyOf(Sets.difference(umsState.getGroups(), ipaState.getGroups())),
            Set.copyOf(Sets.difference(umsState.getUsers(), ipaState.getUsers())),
            groupsToBeRemoved,
            usersToRemove,
            groupMembershipToAdd,
            groupMembershipToRemove);
    }

    private static Set<FmsGroup> getGroupsToBeRemoved(Set<FmsGroup> umsGroups, Set<FmsGroup> ipaGroups) {
        Set<FmsGroup> groupsToBeRemoved = new HashSet<>();

        ipaGroups.forEach(group -> {
            if (!umsGroups.contains(group) && !group.getName().equals("admins")) {
                groupsToBeRemoved.add(group);
            }
        });

        return groupsToBeRemoved;


    }

    private static Set<FmsUser> getUsersToBeRemoved(Collection<String> users, Set<FmsUser> ipaStateUsers) {
        Set<FmsUser> usersToBeRemoved = new HashSet<>();

        ipaStateUsers.forEach(ipaUser -> {
            if (!users.contains(ipaUser.getName())) {
                usersToBeRemoved.add(ipaUser);
            }
        });
        return usersToBeRemoved;
    }
}
