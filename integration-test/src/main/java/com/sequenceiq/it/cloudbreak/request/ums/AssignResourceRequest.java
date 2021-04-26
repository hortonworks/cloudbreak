package com.sequenceiq.it.cloudbreak.request.ums;

public class AssignResourceRequest {

    private String resourceCrn;

    private String roleCrn;

    public AssignResourceRequest() {
    }

    public AssignResourceRequest(String resourceCrn, String roleCrn) {
        this.resourceCrn = resourceCrn;
        this.roleCrn = roleCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getRoleCrn() {
        return roleCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setRoleCrn(String roleCrn) {
        this.roleCrn = roleCrn;
    }
}
