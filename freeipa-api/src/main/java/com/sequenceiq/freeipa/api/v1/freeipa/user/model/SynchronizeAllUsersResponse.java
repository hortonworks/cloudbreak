package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SynchronizeAllUsersV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeAllUsersResponse {
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ID, required = true)
    private final String id;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STATUS, required = true)
    private final SynchronizationStatus status;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_STARTTIME)
    private final String startTime;

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ENDTIME)
    private final String endTime;

    public SynchronizeAllUsersResponse(String id, SynchronizationStatus status, String startTime, String endTime) {
        this.id = requireNonNull(id);
        this.status = requireNonNull(status);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public SynchronizationStatus getStatus() {
        return status;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}
