package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

/**
 * Authorization framework interface for getting resource name list by resource CRN list to allow
 * authz framework to provide name for authz related error messager in order to increase readability
 */
public interface AuthorizationResourceNamesProvider {

    default Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return Map.of();
    }

    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.noneOf(Crn.ResourceType.class);
    }
}
