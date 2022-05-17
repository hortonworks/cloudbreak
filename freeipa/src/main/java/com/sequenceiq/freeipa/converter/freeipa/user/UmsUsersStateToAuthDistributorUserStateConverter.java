package com.sequenceiq.freeipa.converter.freeipa.user;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.Group;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.GroupMembership;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.User;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.User.State;
import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;

@Component
public class UmsUsersStateToAuthDistributorUserStateConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateToAuthDistributorUserStateConverter.class);

    public UserState convert(UmsUsersState umsUsersState) {
        Set<User> users = umsUsersState.getUsersState().getUsers().stream()
                .filter(Objects::nonNull)
                .map(this::convert)
                .collect(Collectors.toSet());
        Set<Group> groups = umsUsersState.getUsersState().getGroups().stream()
                .filter(Objects::nonNull)
                .map(this::convert)
                .collect(Collectors.toSet());
        Map<String, GroupMembership> groupMemberships = umsUsersState.getUsersState().getGroupMembership().asMap().entrySet().stream()
                .filter(e -> ObjectUtils.allNotNull(e, e.getKey(), e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> convert(e.getValue())));
        Map<String, AuthDistributorProto.UserMetadata> userMetadataMap = umsUsersState.getUsersState().getUserMetadataMap().entrySet().stream()
                .filter(e -> ObjectUtils.allNotNull(e, e.getKey(), e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> convert(e.getValue())));
        return UserState.newBuilder()
                .addAllUsers(users)
                .addAllGroups(groups)
                .putAllGroupMemberships(groupMemberships)
                .putAllUserMetadataMap(userMetadataMap)
                .build();
    }

    private User convert(FmsUser fmsUser) {
        User.Builder builder = User.newBuilder();
        if (fmsUser.getName() != null) {
            builder.setName(fmsUser.getName());
        }
        if (fmsUser.getFirstName() != null) {
            builder.setFirstName(fmsUser.getFirstName());
        }
        if (fmsUser.getLastName() != null) {
            builder.setLastName(fmsUser.getLastName());
        }
        if (fmsUser.getState() != null) {
            try {
                builder.setState(State.valueOf(fmsUser.getState().name()));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid state value: {} for fms user: {}", fmsUser.getState(), fmsUser.getName());
            }
        }
        return builder.build();
    }

    private Group convert(FmsGroup fmsGroup) {
        Group.Builder builder = Group.newBuilder();
        if (fmsGroup.getName() != null) {
            builder.setName(fmsGroup.getName());
        }
        return builder.build();
    }

    private GroupMembership convert(Collection<String> members) {
        return GroupMembership.newBuilder()
                .addAllUser(members)
                .build();
    }

    private AuthDistributorProto.UserMetadata convert(UserMetadata umsUserMetadata) {
        AuthDistributorProto.UserMetadata.Builder builder = AuthDistributorProto.UserMetadata.newBuilder();
        if (umsUserMetadata.getCrn() != null) {
            builder.setCrn(umsUserMetadata.getCrn());
        }
        builder.setWorkloadCredentialsVersion(umsUserMetadata.getWorkloadCredentialsVersion());
        return builder.build();
    }
}
