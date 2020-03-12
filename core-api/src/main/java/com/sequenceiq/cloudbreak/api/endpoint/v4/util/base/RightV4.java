package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public enum RightV4 {
    DISTROX_READ(AuthorizationResourceType.DATAHUB.getResource(), AuthorizationResourceAction.READ.getAction()),
    DISTROX_WRITE(AuthorizationResourceType.DATAHUB.getResource(), AuthorizationResourceAction.WRITE.getAction()),
    SDX_READ(AuthorizationResourceType.DATALAKE.getResource(), AuthorizationResourceAction.READ.getAction()),
    SDX_WRITE(AuthorizationResourceType.DATALAKE.getResource(), AuthorizationResourceAction.WRITE.getAction()),
    ENVIRONMENT_READ(AuthorizationResourceType.ENVIRONMENT.getResource(), AuthorizationResourceAction.READ.getAction()),
    ENVIRONMENT_WRITE(AuthorizationResourceType.ENVIRONMENT.getResource(), AuthorizationResourceAction.WRITE.getAction());

    private String resource;

    private String action;

    RightV4(String resource, String action) {
        this.resource = resource;
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}
