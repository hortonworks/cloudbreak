package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatalakeResourcesDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ServiceDescriptorV4Response {
    @ApiModelProperty(DatalakeResourcesDescription.SERVICE_NAME)
    private String serviceName;

    @ApiModelProperty(DatalakeResourcesDescription.BLUEPRINT_PARAMS)
    private Map<String, String> blueprintParams;

    @ApiModelProperty(DatalakeResourcesDescription.COMPONENT_HOSTS)
    private Map<String, String> componentHosts;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getBlueprintParams() {
        return blueprintParams;
    }

    public void setBlueprintParams(Map<String, String> blueprintParams) {
        this.blueprintParams = blueprintParams;
    }

    public Map<String, String> getComponentHosts() {
        return componentHosts;
    }

    public void setComponentHosts(Map<String, String> componentHosts) {
        this.componentHosts = componentHosts;
    }
}
