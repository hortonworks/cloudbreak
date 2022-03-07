package com.sequenceiq.authorization.service;

public interface AuthorizationResourceCrnProvider extends ResourcePropertyProvider {

    String getResourceCrnByResourceName(String resourceName);
}
