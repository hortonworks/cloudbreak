package com.sequenceiq.authorization.service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;

/**
 * Generic authorization framework interface to provide type of resource
 * in order to allow framework to choose proper implementation during permission check
 */
public interface ResourcePropertyProvider {

    AuthorizationResourceType getSupportedAuthorizationResourceType();
}
