package com.sequenceiq.authorization.info.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CheckResourceRightsV4Request extends CheckRightV4Request {

    private List<ResourceRightsV4> resourceRights;

    public List<ResourceRightsV4> getResourceRights() {
        return resourceRights;
    }

    public void setResourceRights(List<ResourceRightsV4> resourceRights) {
        this.resourceRights = resourceRights;
    }
}
