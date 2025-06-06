package com.sequenceiq.freeipa.converter.usersync;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.usersync.UserSyncStatusResponse;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Component
public class UserSyncStatusToUserSyncStatusResponseConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncStatusToUserSyncStatusResponseConverter.class);

    public UserSyncStatusResponse convert(UserSyncStatus userSyncStatus, String environmentCrn) {
        UserSyncStatusResponse response = new UserSyncStatusResponse();
        Optional.ofNullable(userSyncStatus.getLastStartedFullSync()).ifPresent(op -> response.setLastStartedUserSyncId(op.getOperationId()));
        Optional.ofNullable(userSyncStatus.getLastSuccessfulFullSync()).ifPresent(op -> response.setLastSuccessfulUserSyncId(op.getOperationId()));
        Optional.ofNullable(userSyncStatus.getUmsEventGenerationIds()).ifPresent(eids -> {
            try {
                response.setEventGenerationIds(eids.get(UmsEventGenerationIds.class).getEventGenerationIds());
            } catch (IOException e) {
                LOGGER.warn("Failed to convert event generation ids [{}] for environment '{}'",
                        userSyncStatus.getUmsEventGenerationIds(), environmentCrn);
            }
        });
        return response;
    }
}
