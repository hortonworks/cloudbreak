package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public interface ResourceNameProvider {

    Map<String, Optional<String>> getNamesByCrns(Collection<String> crns);

    EnumSet<Crn.ResourceType> getCrnTypes();
}
