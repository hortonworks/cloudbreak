package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public interface AuthorizationResourceNamesProvider {

    default Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return Map.of();
    }

    default EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.noneOf(Crn.ResourceType.class);
    }
}
