package com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain;

import java.util.HashSet;
import java.util.Set;

public class Dependencies {

    private Set<String> services = new HashSet<>();

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }
}
