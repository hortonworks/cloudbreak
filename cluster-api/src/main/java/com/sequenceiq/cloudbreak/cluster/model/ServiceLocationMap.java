package com.sequenceiq.cloudbreak.cluster.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceLocationMap {

    private Map<String, ServiceLocation> serviceLocations = new HashMap<>();

    public ServiceLocationMap add(ServiceLocation serviceLocation) {
        serviceLocations.put(serviceLocation.getService(), serviceLocation);
        return this;
    }

    public ServiceLocation getServiceLocationByService(String service) {
        return serviceLocations.get(service);
    }

    public String getVolumePathByService(String service) {
        Optional<ServiceLocation> serviceLocation = Optional.ofNullable(serviceLocations.get(service));
        return serviceLocation.map(ServiceLocation::getVolumePath).orElse(null);
    }

    public Set<String> getAllVolumePath() {
        return serviceLocations.values().stream().map(ServiceLocation::getVolumePath).collect(Collectors.toSet());
    }
}
