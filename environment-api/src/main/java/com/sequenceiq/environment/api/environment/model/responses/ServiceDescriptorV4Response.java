package com.sequenceiq.environment.api.environment.model.responses;

import java.util.Map;

import com.sequenceiq.environment.api.environment.doc.EnvironmentDatalakeDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ServiceDescriptorV4Response {
    @ApiModelProperty(EnvironmentDatalakeDescription.SERVICE_NAME)
    private String serviceName;

    @ApiModelProperty(EnvironmentDatalakeDescription.BLUEPRINT_PARAMS)
    private Map<String, String> blueprintParams;

    @ApiModelProperty(EnvironmentDatalakeDescription.COMPONENT_HOSTS)
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
