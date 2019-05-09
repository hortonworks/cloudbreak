package com.sequenceiq.environment.api.environment.model.response;

import java.util.Map;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ServiceDescriptorV1Response {
    @ApiModelProperty(EnvironmentModelDescription.SERVICE_NAME)
    private String serviceName;

    @ApiModelProperty(EnvironmentModelDescription.BLUEPRINT_PARAMS)
    private Map<String, String> blueprintParams;

    @ApiModelProperty(EnvironmentModelDescription.COMPONENT_HOSTS)
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
