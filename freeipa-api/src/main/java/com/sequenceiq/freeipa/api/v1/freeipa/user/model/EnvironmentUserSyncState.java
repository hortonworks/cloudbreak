package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentUserSyncV1State")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentUserSyncState {
    @NotNull
    @Schema(description = UserModelDescriptions.USERSYNC_STATE, required = true)
    private UserSyncState state;

    @Schema(description = UserModelDescriptions.USERSYNC_ID)
    private String lastUserSyncOperationId;

    public UserSyncState getState() {
        return state;
    }

    public void setState(UserSyncState state) {
        this.state = state;
    }

    public String getLastUserSyncOperationId() {
        return lastUserSyncOperationId;
    }

    public void setLastUserSyncOperationId(String lastUserSyncOperationId) {
        this.lastUserSyncOperationId = lastUserSyncOperationId;
    }

    @Override
    public String toString() {
        return "EnvironmentUserSyncState{" +
                "state=" + state +
                ", lastUserSyncOperationId='" + lastUserSyncOperationId + '\'' +
                '}';
    }
}
