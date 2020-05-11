package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

public enum RightV4 {
    DISTROX_READ(AuthorizationResourceAction.DATAHUB_READ),
    DISTROX_WRITE(AuthorizationResourceAction.DATAHUB_WRITE),
    SDX_READ(AuthorizationResourceAction.DATALAKE_READ),
    SDX_WRITE(AuthorizationResourceAction.DATALAKE_WRITE),
    ENVIRONMENT_READ(AuthorizationResourceAction.ENVIRONMENT_READ),
    ENVIRONMENT_WRITE(AuthorizationResourceAction.ENVIRONMENT_WRITE);

    private AuthorizationResourceAction action;

    RightV4(AuthorizationResourceAction action) {
        this.action = action;
    }

    public AuthorizationResourceAction getAction() {
        return action;
    }
}
