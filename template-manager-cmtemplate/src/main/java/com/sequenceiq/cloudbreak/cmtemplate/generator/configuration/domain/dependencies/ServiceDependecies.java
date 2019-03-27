package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies;

import java.util.HashSet;
import java.util.Set;

public class ServiceDependecies {

    private Set<ServiceConfig> services = new HashSet<>();

    public Set<ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Set<ServiceConfig> services) {
        this.services = services;
    }
}
