package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceModeJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.STATUS_MAINTENANCE_MODE)
    private MaintenanceModeStatus status;

    public MaintenanceModeStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceModeStatus status) {
        this.status = status;
    }
}
