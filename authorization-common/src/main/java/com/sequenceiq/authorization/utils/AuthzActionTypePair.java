package com.sequenceiq.authorization.utils;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public class AuthzActionTypePair {

    private final AuthorizationResourceType resourceType;

    private final AuthorizationResourceAction resourceAction;

    public AuthzActionTypePair(AuthorizationResourceType resourceType, AuthorizationResourceAction resourceAction) {
        this.resourceAction = resourceAction;
        this.resourceType = resourceType;
    }

    public AuthorizationResourceType getResourceType() {
        return resourceType;
    }

    public AuthorizationResourceAction getResourceAction() {
        return resourceAction;
    }

    public boolean hasNoAction() {
        return resourceAction == null;
    }

    public boolean hasAction() {
        return resourceAction != null;
    }

}
