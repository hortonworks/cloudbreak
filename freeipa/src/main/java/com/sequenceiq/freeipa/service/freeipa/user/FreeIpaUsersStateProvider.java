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
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.FreeIpaUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.FreeIpaUsersState.Builder;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

import javax.inject.Inject;

@Service
public class FreeIpaUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUsersStateProvider.class);

    @Inject
    private UserMetadataConverter userMetadataConverter;

    public FreeIpaUsersState getUsersState(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        LOGGER.debug("Retrieving all users from FreeIPA");
        FreeIpaUsersState.Builder ipaUsersStateBuilder = new Builder();
        UsersState.Builder usersStateBuilder = new UsersState.Builder();

        freeIpaClient.userFindAll().stream()
                .filter(user -> !IPA_PROTECTED_USERS.contains(user.getUid()))
                .forEach(user -> {
                    usersStateBuilder.addUser(fromIpaUser(user));
                    userMetadataConverter.toUserMetadata(user).ifPresent(meta -> ipaUsersStateBuilder.addUserMetadata(user.getUid(), meta));

                    if (null != user.getMemberOfGroup()) {
                        user.getMemberOfGroup().stream()
                                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group))
                                .forEach(group -> usersStateBuilder.addMemberToGroup(group, user.getUid()));
                    } else {
                        LOGGER.warn("User {} is not a member of any groups.", user.getUid());
                    }
                });

        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group.getCn()))
                .forEach(group -> usersStateBuilder.addGroup(fromIpaGroup(group)));

        ipaUsersStateBuilder.setUsersState(usersStateBuilder.build());
        return ipaUsersStateBuilder.build();
    }

    public FreeIpaUsersState getFilteredFreeIpaState(FreeIpaClient freeIpaClient, Set<String> userNames)
            throws FreeIpaClientException {
        LOGGER.debug("Retrieving users with user names [{}] from FreeIPA", userNames);
        FreeIpaUsersState.Builder ipaUsersStateBuilder = new Builder();
        UsersState.Builder usersStateBuilder = new UsersState.Builder();

        // get all groups from IPA
        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group.getCn()))
                .forEach(group -> usersStateBuilder.addGroup(fromIpaGroup(group)));

        for (String userName : userNames) {
            if (IPA_PROTECTED_USERS.contains(userName)) {
                continue;
            }
            Optional<com.sequenceiq.freeipa.client.model.User> ipaUserOptional = freeIpaClient.userFind(userName);
            if (ipaUserOptional.isPresent()) {
                com.sequenceiq.freeipa.client.model.User ipaUser = ipaUserOptional.get();
                usersStateBuilder.addUser(fromIpaUser(ipaUser));
                userMetadataConverter.toUserMetadata(ipaUser).ifPresent(meta -> ipaUsersStateBuilder.addUserMetadata(ipaUser.getUid(), meta));
                if (ipaUser.getMemberOfGroup() != null) {
                    ipaUser.getMemberOfGroup().stream()
                            .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group))
                            .forEach(groupname -> {
                                usersStateBuilder.addMemberToGroup(groupname, userName);
                            });
                }
            }
        }

        ipaUsersStateBuilder.setUsersState(usersStateBuilder.build());
        return ipaUsersStateBuilder.build();
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
