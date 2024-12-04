package com.sequenceiq.authorization.info.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CheckResourceRightV4SingleResponse {

    private String resourceCrn;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CheckRightV4SingleResponse> rights = new ArrayList<>();

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
