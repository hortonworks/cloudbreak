package com.sequenceiq.cloudbreak.api.service;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposedServices {

    private Set<ExposedService> services = new HashSet<>();

    public Set<ExposedService> getServices() {
        return services;
    }

    public void setServices(Set<ExposedService> services) {
        this.services = services;
    }
}
