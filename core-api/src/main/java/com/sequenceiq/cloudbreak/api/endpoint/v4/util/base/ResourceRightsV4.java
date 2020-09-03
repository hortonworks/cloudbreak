package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ResourceRightsV4 {

    private String resourceCrn;

    private List<RightV4> rights;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public List<RightV4> getRights() {
        return rights;
    }

    public void setRights(List<RightV4> rights) {
        this.rights = rights;
    }
}
