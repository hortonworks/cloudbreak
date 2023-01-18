package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceModeV4Request implements JsonEntity {

    @Schema(description = ClusterModelDescription.STATUS_MAINTENANCE_MODE)
    private MaintenanceModeStatus status;

    public MaintenanceModeStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceModeStatus status) {
        this.status = status;
    }
}
