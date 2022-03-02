package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface EnvironmentPropertyProvider extends ResourcePropertyProvider {

    default Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        return Optional.of(resourceCrn);
    }

    default Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        return resourceCrns.stream().collect(Collectors.toMap(crn -> crn, crn -> Optional.of(crn)));
    }

    default Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.ENVIRONMENT);
    }

    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }
}
