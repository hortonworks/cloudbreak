package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

public class StackEndpointV4Response {

    @Schema
    private String crn;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Collection<ClusterExposedServiceV4Response>> endpoints = new HashMap<>();

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Collection<ClusterExposedServiceV4Response>> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public String toString() {
        return "StackEndpointV4Response{" +
                "crn='" + crn + '\'' +
                ", endpoints=" + endpoints +
                '}';
    }
}
