package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceModeV4Request implements JsonEntity {

    @ApiModelProperty(value = ClusterModelDescription.STATUS_MAINTENANCE_MODE, allowableValues = "ENABLED,VALIDATION_REQUESTED,DISABLED")
    private MaintenanceModeStatus status;

    public MaintenanceModeStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceModeStatus status) {
        this.status = status;
    }
}
