package com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.versionmatrix;

import java.util.HashSet;
import java.util.Set;

public class ServiceList {

    private Set<String> services = new HashSet<>();

    private String version;

    private String stackType;

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStackType() {
        return stackType;
    }

    public void setStackType(String stackType) {
        this.stackType = stackType;
    }
}
