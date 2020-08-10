package com.sequenceiq.authorization.resource;

import java.io.Serializable;

public class AuthorizationResourceActionModel implements Serializable {

    private String right;

    private String legacyRight;

    private AuthorizationResourceType resourceType;

    public String getRight() {
        return right;
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

    public String getLegacyRight() {
        return legacyRight;
    }

    public void setLegacyRight(String legacyRight) {
        this.legacyRight = legacyRight;
    }
}
