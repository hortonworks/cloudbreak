package com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain;

import java.util.HashSet;
import java.util.Set;

public class SupportedServices {

    private Set<SupportedService> services = new HashSet<>();

    public Set<SupportedService> getServices() {
        return services;
    }

    public void setServices(Set<SupportedService> services) {
        this.services = services;
    }
}
