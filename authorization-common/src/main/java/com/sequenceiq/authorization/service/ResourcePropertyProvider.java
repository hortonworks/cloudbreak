package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public interface ResourcePropertyProvider {

    default String getResourceCrnByResourceName(String resourceName) {
        throw new NotImplementedException("Logic for getting resource CRN by resource name should have been implemented for authorization!");
    }

    default List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        throw new NotImplementedException("Logic for getting resource CRN list by resource name list should have been implemented for authorization!");
    }

    default Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return Optional.empty();
    }

    default Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        return Map.of();
    }

    Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType();

    default Map<String, Optional<String>> getNamesByCrns(Collection<String> crns) {
        return Map.of();
    }

    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.noneOf(Crn.ResourceType.class);
    }
}
