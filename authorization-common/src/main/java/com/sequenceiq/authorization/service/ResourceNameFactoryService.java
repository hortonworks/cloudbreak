package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class ResourceNameFactoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNameFactoryService.class);

    @Inject
    private Optional<List<ResourceNameProvider>> resourceNameProviderList;

    public Map<String, Optional<String>> getNames(Collection<String> resourceCrns) {
        Map<String, Optional<String>> result = new HashMap<>();
        sortByResourceType(resourceCrns)
                .forEach((type, crns) -> result.putAll(getNamesForType(type, crns)));
        resourceCrns.stream().forEach(crn -> result.putIfAbsent(crn, Optional.empty()));
        return result;
    }

    private Map<String, Optional<String>> getNamesForType(Crn.ResourceType resourceType, Collection<String> resourceCrns) {
        return resourceNameProviderList.orElse((List<ResourceNameProvider>) Collections.EMPTY_LIST).stream()
                .filter(provider -> provider.getCrnTypes().contains(resourceType))
                .findFirst()
                .map(provider -> provider.getNamesByCrns(resourceCrns))
                .orElse(resourceCrns.stream().collect(Collectors.toMap(crn -> crn, crn -> Optional.empty())));
    }

    private Map<Crn.ResourceType, Collection<String>> sortByResourceType(Collection<String> resourceCrns) {
        Map<Crn.ResourceType, Collection<String>> result = new EnumMap<>(Crn.ResourceType.class);
        for (String resourceCrn : resourceCrns) {
            Crn crn = Crn.fromString(resourceCrn);
            if (crn != null) {
                result.computeIfAbsent(crn.getResourceType(), c -> new HashSet<>()).add(resourceCrn);
            }
        }
        return result;
    }

}
