package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.GatewayModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GatewayTopologyV4Response implements JsonEntity {

    @ApiModelProperty(GatewayModelDescription.KNOX_TOPOLOGY_NAME)
    private String topologyName;

    @ApiModelProperty(GatewayModelDescription.EXPOSED_KNOX_SERVICES)
    private List<String> exposedServices;

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
