package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public class GcpTargetGroupResponse implements Serializable {

    @Schema(description = StackModelDescription.GCP_LB_INSTANCE_GROUP, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String gcpInstanceGroupName;

    @Schema(description = StackModelDescription.GCP_LB_BACKEND_SERVICE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String gcpBackendServiceName;

    public String getGcpInstanceGroupName() {
        return gcpInstanceGroupName;
    }

    public void setGcpInstanceGroupName(String gcpInstanceGroupName) {
        this.gcpInstanceGroupName = gcpInstanceGroupName;
    }

    public String getGcpBackendServiceName() {
        return gcpBackendServiceName;
    }

    public void setGcpBackendServiceName(String gcpBackendServiceName) {
        this.gcpBackendServiceName = gcpBackendServiceName;
    }
}
