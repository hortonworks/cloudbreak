package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.usersync;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UserSyncStatusV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSyncStatusResponse {
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID)
    private String lastRequestedUserSyncId;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID)
    private String lastSuccessfulUserSyncId;

    @NotNull
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID)
    private Map<String, String> eventGenerationIds;

    public String getLastRequestedUserSyncId() {
        return lastRequestedUserSyncId;
    }

    public void setLastRequestedUserSyncId(String lastRequestedUserSyncId) {
        this.lastRequestedUserSyncId = lastRequestedUserSyncId;
    }

    public String getLastSuccessfulUserSyncId() {
        return lastSuccessfulUserSyncId;
    }

    public void setLastSuccessfulUserSyncId(String lastSuccessfulUserSyncId) {
        this.lastSuccessfulUserSyncId = lastSuccessfulUserSyncId;
    }

    public Map<String, String> getEventGenerationIds() {
        return eventGenerationIds;
    }

    public void setEventGenerationIds(Map<String, String> eventGenerationIds) {
        this.eventGenerationIds = ImmutableMap.copyOf(eventGenerationIds);
    }
}
