package com.sequenceiq.it.cloudbreak.request.ums;

import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;

public class AssignResourceRequest {

    private String resourceCrn;

    private UmsResourceRole umsResourceRole;

    public AssignResourceRequest() {
    }

    public AssignResourceRequest(String resourceCrn, UmsResourceRole umsResourceRole) {
        this.resourceCrn = resourceCrn;
        this.umsResourceRole = umsResourceRole;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public UmsResourceRole getUmsResourceRole() {
        return umsResourceRole;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setUmsResourceRole(UmsResourceRole umsResourceRole) {
        this.umsResourceRole = umsResourceRole;
    }
}
