package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.Group;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.User;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@Service
public class FreeIpaUsersStateProvider {

    // TODO add other Cloudera-managed users (e.g., Kerberos and LDAP users?)
    @VisibleForTesting
    static final List<String> IPA_ONLY_USERS = List.of("admin");

    // TODO add other Cloudera-managed groups?
    // TODO handle name conflicts between ipa and ums? e.g., should ums "admins" be ipa "admins"?
    @VisibleForTesting
    static final List<String> IPA_ONLY_GROUPS = List.of("admins", "editors", "ipausers", "trust admins");

    public UsersState getUsersState(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        UsersState.Builder builder = new UsersState.Builder();

        freeIpaClient.userFindAll().stream()
                .filter(user -> !IPA_ONLY_USERS.contains(user.getUid()))
                .forEach(user -> {
                    builder.addUser(fromIpaUser(user));
                    user.getMemberOfGroup().stream()
                            .filter(group -> !IPA_ONLY_GROUPS.contains(group))
                            .forEach(group -> builder.addMemberToGroup(group, user.getUid()));
                });

        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_ONLY_GROUPS.contains(group.getCn()))
                .forEach(group -> builder.addGroup(fromIpaGroup(group)));

        return builder.build();
    }

    public UsersState getFilteredUsersState(FreeIpaClient freeIpaClient, Set<String> users) throws FreeIpaClientException {
        UsersState.Builder builder = new UsersState.Builder();

        for (String username : users) {
            if (IPA_ONLY_USERS.contains(username)) {
                continue;
            }
            Optional<com.sequenceiq.freeipa.client.model.User> ipaUserOptional = freeIpaClient.userFind(username);
            if (ipaUserOptional.isPresent()) {
                com.sequenceiq.freeipa.client.model.User ipaUser = ipaUserOptional.get();
                builder.addUser(fromIpaUser(ipaUser));
                ipaUser.getMemberOfGroup().stream()
                        .filter(group -> !IPA_ONLY_GROUPS.contains(group))
                        .forEach(groupname -> {
                            Group group = new Group();
                            group.setName(groupname);
                            builder.addGroup(group);
                            builder.addMemberToGroup(groupname, username);
                        });
            }
        }

        return builder.build();
    }

    @VisibleForTesting
    User fromIpaUser(com.sequenceiq.freeipa.client.model.User ipaUser) {
        User user = new User();
        user.setName(ipaUser.getUid());
        user.setFirstName(ipaUser.getGivenname());
        user.setLastName(ipaUser.getSn());
        return user;
    }

    @VisibleForTesting
    Group fromIpaGroup(com.sequenceiq.freeipa.client.model.Group ipaGroup) {
        Group group = new Group();
        group.setName(ipaGroup.getCn());
        return group;
    }
}
