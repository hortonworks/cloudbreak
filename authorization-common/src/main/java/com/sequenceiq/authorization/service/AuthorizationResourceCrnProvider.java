package com.sequenceiq.authorization.service;

/**
 * Authorization framework interface for getting resource CRN by resource name to allow
 * authz framework to execute permission check based on name
 */
public interface AuthorizationResourceCrnProvider extends ResourcePropertyProvider {

    String getResourceCrnByResourceName(String resourceName);
}
