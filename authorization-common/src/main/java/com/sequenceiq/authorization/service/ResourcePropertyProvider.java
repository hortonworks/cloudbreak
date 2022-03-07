package com.sequenceiq.authorization.service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public interface ResourcePropertyProvider {

    AuthorizationResourceType getSupportedAuthorizationResourceType();
}
