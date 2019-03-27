package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.HashSet;
import java.util.Set;

public class ServiceDependencyMatrixV4Response {

    private Set<String> dependencies = new HashSet<>();

    private Set<String> services = new HashSet<>();

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }
}
