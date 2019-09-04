package com.sequenceiq.authorization.service;

import org.apache.commons.lang3.NotImplementedException;

public interface ResourceBasedEnvironmentCrnProvider<T extends Object> {

    default String getEnvironmentCrnByResourceName(String resourceName) {
        throw new NotImplementedException("Logic for getting environment CRN by resource name should have been implemented for authorization!");
    }

    default String getEnvironmentCrnByResourceCrn(String resourceCrn) {
        throw new NotImplementedException("Logic for getting environment CRN by resource CRN should have been implemented for authorization!");
    }

    Class<T> supportedResourceClass();
}
