package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class GetAuditEventRequestV4Filter {

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
