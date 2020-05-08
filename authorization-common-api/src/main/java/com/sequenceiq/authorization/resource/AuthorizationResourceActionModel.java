package com.sequenceiq.authorization.resource;

import java.io.Serializable;

public class AuthorizationResourceActionModel implements Serializable {

    private String right;

    private String legacyRight;

    private AuthorizationResourceType resourceType;

    private AuthorizationResourceActionType actionType;

    public String getRight() {
        return right;
    }

    public AuthorizationResourceActionType getActionType() {
        return actionType;
    }

    public AuthorizationResourceType getResourceType() {
        return resourceType;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public void setResourceType(AuthorizationResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setActionType(AuthorizationResourceActionType actionType) {
        this.actionType = actionType;
    }

    public String getLegacyRight() {
        return legacyRight;
    }

    public void setLegacyRight(String legacyRight) {
        this.legacyRight = legacyRight;
    }
}
