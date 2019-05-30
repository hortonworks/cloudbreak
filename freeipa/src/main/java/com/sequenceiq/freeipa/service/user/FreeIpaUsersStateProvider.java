package com.sequenceiq.freeipa.service.user;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.user.model.UsersState;

@Service
public class FreeIpaUsersStateProvider {

    // TODO add other Cloudera-managed users (e.g., Kerberos and LDAP users?)
    @VisibleForTesting
    static final List<String> IPA_ONLY_USERS = List.of("admin");

    // TODO add other Cloudera-managed groups?
    @VisibleForTesting
    static final List<String> IPA_ONLY_GROUPS = List.of("admins", "editors", "ipausers", "trust admins");

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    // TODO improve out exception handling
    public UsersState getUsersState(Stack stack) throws Exception {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);

        Set<User> users = freeIpaClient.userFindAll().stream()
                .filter(user -> !IPA_ONLY_USERS.contains(user.getUid()))
                .map(this::fromIpaUser)
                .collect(Collectors.toSet());

        Set<Group> groups = freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_ONLY_GROUPS.contains(group.getCn()))
                .map(this::fromIpaGroup)
                .collect(Collectors.toSet());

        return new UsersState(groups, users);
    }

    @VisibleForTesting
    User fromIpaUser(com.sequenceiq.freeipa.client.model.User ipaUser) {
        User user = new User();
        user.setName(ipaUser.getUid());
        user.setFirstName(ipaUser.getGivenname());
        user.setLastName(ipaUser.getSn());
        user.setGroups(ipaUser.getMemberOfGroup().stream()
                .filter(group -> !IPA_ONLY_GROUPS.contains(group))
                .collect(Collectors.toSet()));
        return user;
    }

    @VisibleForTesting
    Group fromIpaGroup(com.sequenceiq.freeipa.client.model.Group ipaGroup) {
        Group group = new Group();
        group.setName(ipaGroup.getCn());
        return group;
    }
}
