package com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain;

public class ServiceDependencyMatrix {

    private Dependencies dependencies;

    private Services services;

    public Dependencies getDependencies() {
        return dependencies;
    }

    public void setDependencies(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}
