package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostGroupServicesV4Response {

    @ApiModelProperty
    private Set<String> servicesOnHostGroup;

    public Set<String> getServicesOnHostGroup() {
        return servicesOnHostGroup;
    }

    public void setServicesOnHostGroup(Set<String> servicesOnHostGroup) {
        this.servicesOnHostGroup = servicesOnHostGroup;
    }
}
