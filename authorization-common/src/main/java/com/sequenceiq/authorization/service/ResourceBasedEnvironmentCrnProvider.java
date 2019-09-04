package com.sequenceiq.authorization.service;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

public interface ResourceBasedEnvironmentCrnProvider {

    default String getEnvironmentCrnByResourceName(String resourceName) {
        throw new NotImplementedException("Logic for getting environment CRN by resource name should have been implemented for authorization!");
    }

    default String getEnvironmentCrnByResourceCrn(String resourceCrn) {
        throw new NotImplementedException("Logic for getting environment CRN by resource CRN should have been implemented for authorization!");
    }

    default List<String> getEnvironmentCrnListByResourceCrnList(List<String> resourceCrns) {
        throw new NotImplementedException("Logic for getting environment CRN list by resource CRN list should have been implemented for authorization!");
    }

    default List<String> getEnvironmentCrnListByResourceNameList(List<String> resourceNames) {
        throw new NotImplementedException("Logic for getting environment CRN list by resource name list should have been implemented for authorization!");
    }
}
