package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.List;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckResourceRightV4SingleResponse {

    private String resourceCrn;

    private List<CheckRightV4SingleResponse> rights;

    public CheckResourceRightV4SingleResponse() {
    }

    public CheckResourceRightV4SingleResponse(String resourceCrn, List<CheckRightV4SingleResponse> rights) {
        this.resourceCrn = resourceCrn;
        this.rights = rights;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public List<CheckRightV4SingleResponse> getRights() {
        return rights;
    }

    public void setRights(List<CheckRightV4SingleResponse> rights) {
        this.rights = rights;
    }
}
