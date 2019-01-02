package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.requests;

import javax.ws.rs.QueryParam;

public class GetAuditEventRequest {

    @QueryParam("resourceType")
    private String resourceType;

    @QueryParam("resourceId")
    private Long resourceId;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }
}
