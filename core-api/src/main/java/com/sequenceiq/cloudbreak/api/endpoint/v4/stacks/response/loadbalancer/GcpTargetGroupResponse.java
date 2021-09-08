package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class GcpTargetGroupResponse implements Serializable {

    @ApiModelProperty(StackModelDescription.GCP_LB_INSTANCE_GROUP)
    @NotNull
    private String gcpInstanceGroupName;

    @ApiModelProperty(StackModelDescription.GCP_LB_BACKEND_SERVICE)
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
