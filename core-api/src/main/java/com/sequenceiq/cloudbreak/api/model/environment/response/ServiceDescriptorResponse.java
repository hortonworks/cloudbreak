package com.sequenceiq.cloudbreak.api.model.environment.response;

import java.util.Map;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ServiceDescriptorResponse {
    @ApiModelProperty(ModelDescriptions.DatalakeResourcesDescription.SERVICE_NAME)
    private String serviceName;

    @ApiModelProperty(ModelDescriptions.DatalakeResourcesDescription.BLUEPRINT_PARAMS)
    private Map<String, String> blueprintParams;

    @ApiModelProperty(ModelDescriptions.DatalakeResourcesDescription.COMPONENT_HOSTS)
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
