package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_PROTECTED_USERS;
import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_UNMANAGED_GROUPS;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@Service
public class FreeIpaUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUsersStateProvider.class);

    public UsersState getUsersState(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        LOGGER.debug("Retrieving all users from FreeIPA");
        UsersState.Builder builder = new UsersState.Builder();

        freeIpaClient.userFindAll().stream()
                .filter(user -> !IPA_PROTECTED_USERS.contains(user.getUid()))
                .forEach(user -> {
                    builder.addUser(fromIpaUser(user));
                    if (null != user.getMemberOfGroup()) {
                        user.getMemberOfGroup().stream()
                                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group))
                                .forEach(group -> builder.addMemberToGroup(group, user.getUid()));
                    } else {
                        LOGGER.warn("User {} is not a member of any groups.", user.getUid());
                    }
                });

        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group.getCn()))
                .forEach(group -> builder.addGroup(fromIpaGroup(group)));

        return builder.build();
    }

    public UsersState getFilteredFreeIPAState(FreeIpaClient freeIpaClient, Set<FmsUser> users) throws FreeIpaClientException {
        LOGGER.debug("Retrieving users with user ids [{}] from FreeIPA", users);
        UsersState.Builder builder = new UsersState.Builder();

        // get all groups from IPA
        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group.getCn()))
                .forEach(group -> builder.addGroup(fromIpaGroup(group)));

        for (FmsUser user : users) {
            if (IPA_PROTECTED_USERS.contains(user.getName())) {
                continue;
            }
            Optional<com.sequenceiq.freeipa.client.model.User> ipaUserOptional = freeIpaClient.userFind(user.getName());
            if (ipaUserOptional.isPresent()) {
                com.sequenceiq.freeipa.client.model.User ipaUser = ipaUserOptional.get();
                builder.addUser(fromIpaUser(ipaUser));
                if (ipaUser.getMemberOfGroup() != null) {
                    ipaUser.getMemberOfGroup().stream()
                            .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group))
                            .forEach(groupname -> {
                                builder.addMemberToGroup(groupname, user.getName());
                            });
                }
            }
        }

        return builder.build();
    }

    @VisibleForTesting
    FmsUser fromIpaUser(com.sequenceiq.freeipa.client.model.User ipaUser) {
        return new FmsUser()
                .withName(ipaUser.getUid())
                .withFirstName(ipaUser.getGivenname())
                .withLastName(ipaUser.getSn());
    }

    @VisibleForTesting
    FmsGroup fromIpaGroup(com.sequenceiq.freeipa.client.model.Group ipaGroup) {
        return new FmsGroup()
                .withName(ipaGroup.getCn());
    }
}
