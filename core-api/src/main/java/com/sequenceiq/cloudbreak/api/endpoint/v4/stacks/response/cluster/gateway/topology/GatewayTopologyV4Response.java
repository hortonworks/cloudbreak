package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.GatewayModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GatewayTopologyV4Response implements JsonEntity {

    @Schema(description = GatewayModelDescription.KNOX_TOPOLOGY_NAME)
    private String topologyName;

    @Schema(description = GatewayModelDescription.EXPOSED_KNOX_SERVICES, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> exposedServices = new ArrayList<>();

    public String getTopologyName() {
        return topologyName;
    }

    public void setTopologyName(String topologyName) {
        this.topologyName = topologyName;
    }

    public List<String> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(List<String> exposedServices) {
        this.exposedServices = exposedServices;
    }
}
