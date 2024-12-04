package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public class GcpLoadBalancerResponse implements Serializable {

    @Schema(description = StackModelDescription.GCP_LOAD_BALANCER_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
