package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_PROTECTED_USERS;
import static com.sequenceiq.freeipa.client.FreeIpaChecks.IPA_UNMANAGED_GROUPS;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@Service
public class FreeIpaUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUsersStateProvider.class);

    @Inject
    private UserMetadataConverter userMetadataConverter;

    public UsersState getUsersState(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        LOGGER.debug("Retrieving all users from FreeIPA");
        UsersState.Builder builder = new UsersState.Builder();

        freeIpaClient.userFindAll().stream()
                .filter(user -> !IPA_PROTECTED_USERS.contains(user.getUid()))
                .forEach(user -> {
                    Optional<UserMetadata> userMetadata = userMetadataConverter.toUserMetadata(user);
                    builder.addUser(fromIpaUser(user, userMetadata));
                    userMetadata.ifPresent(meta -> builder.addUserMetadata(user.getUid(), meta));
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

    public UsersState getFilteredFreeIpaState(FreeIpaClient freeIpaClient, Set<String> userNames)
            throws FreeIpaClientException {
        LOGGER.debug("Retrieving users with user names [{}] from FreeIPA", userNames);
        UsersState.Builder builder = new UsersState.Builder();

        freeIpaClient.groupFindAll().stream()
                .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group.getCn()))
                .forEach(group -> builder.addGroup(fromIpaGroup(group)));

        for (String userName : userNames) {
            if (IPA_PROTECTED_USERS.contains(userName)) {
                continue;
            }
            Optional<User> userOptional = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(
                    () -> freeIpaClient.userShow(userName), null);
            userOptional.ifPresent(ipaUser -> {
                Optional<UserMetadata> userMetadata = userMetadataConverter.toUserMetadata(ipaUser);
                builder.addUser(fromIpaUser(ipaUser, userMetadata));
                userMetadata.ifPresent(meta -> builder.addUserMetadata(ipaUser.getUid(), meta));
                if (ipaUser.getMemberOfGroup() != null) {
                    ipaUser.getMemberOfGroup().stream()
                            .filter(group -> !IPA_UNMANAGED_GROUPS.contains(group))
                            .forEach(groupname -> {
                                builder.addMemberToGroup(groupname, userName);
                            });
                }
            });
        }

        return builder.build();
    }

    @VisibleForTesting
    FmsUser fromIpaUser(com.sequenceiq.freeipa.client.model.User ipaUser, Optional<UserMetadata> userMetadata) {
        FmsUser fmsUser = new FmsUser()
                .withName(ipaUser.getUid())
                .withFirstName(ipaUser.getGivenname())
                .withLastName(ipaUser.getSn())
                .withState(ipaUser.getNsAccountLock() ? FmsUser.State.DISABLED : FmsUser.State.ENABLED);
        if (userMetadata.isPresent()) {
            fmsUser.withCrn(userMetadata.get().getCrn());
        }
        return fmsUser;
    }

    @VisibleForTesting
    FmsGroup fromIpaGroup(com.sequenceiq.freeipa.client.model.Group ipaGroup) {
        return new FmsGroup()
                .withName(ipaGroup.getCn());
    }
}
