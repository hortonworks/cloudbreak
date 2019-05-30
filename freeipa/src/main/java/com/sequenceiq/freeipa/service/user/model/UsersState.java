package com.sequenceiq.freeipa.service.user.model;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;

public class UsersState {
    private Set<Group> groups;

    private Set<User> users;

    public UsersState(Set<Group> groups, Set<User> users) {
        this.groups = requireNonNull(groups);
        this.users = requireNonNull(users);
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<User> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return "UsersState{"
                + "groups=" + groups
                + ", users=" + users
                + '}';
    }
}