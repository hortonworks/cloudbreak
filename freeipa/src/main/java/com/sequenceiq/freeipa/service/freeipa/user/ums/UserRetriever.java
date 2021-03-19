package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@Component
public class UserRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRetriever.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public List<UserManagementProto.User> getUsers(
            String actorCrn, String accountId,
            Optional<String> requestIdOptional,
            boolean fullSync, Set<String> userCrns,
            BiConsumer<String, String> warnings) {
        if (fullSync) {
            return grpcUmsClient.listAllUsers(actorCrn, accountId, requestIdOptional);
        } else if (!userCrns.isEmpty()) {
            return getRequestedUsers(actorCrn, accountId, userCrns, requestIdOptional, warnings);
        } else {
            return List.of();
        }
    }

    private List<UserManagementProto.User> getRequestedUsers(
            String actorCrn, String accountId,
            Set<String> userCrns, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        try {
            return grpcUmsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns),
                    requestIdOptional);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Some requested user was not found in UMS. " +
                                "Attempting to retrieve users individually.",
                        e.getLocalizedMessage());
                return getRequestedUsersIndividually(actorCrn, accountId, userCrns, requestIdOptional, warnings);
            } else {
                throw e;
            }
        }
    }

    private List<UserManagementProto.User> getRequestedUsersIndividually(
            String actorCrn, String accountId,
            Set<String> userCrns, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        List<UserManagementProto.User> users = Lists.newArrayListWithCapacity(userCrns.size());
        for (String userCrn : userCrns) {
            try {
                users.addAll(grpcUmsClient.listUsers(actorCrn, accountId, List.of(userCrn),
                        requestIdOptional));
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                    LOGGER.warn("User CRN {} not found in UMS. User will not be added to the UMS Users State. {}",
                            userCrn, e.getLocalizedMessage());
                    warnings.accept(userCrn, String.format("User %s not found.", userCrn));
                } else {
                    throw e;
                }
            }
        }
        return users;
    }
}
