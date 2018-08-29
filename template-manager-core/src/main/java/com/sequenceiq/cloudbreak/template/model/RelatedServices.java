package com.sequenceiq.cloudbreak.template.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RelatedServices {

    private Set<String> services;

    @JsonCreator
    public RelatedServices(@JsonProperty(value = "services", required = true) Set<String> services) {
        this.services = services;
    }

    public Set<String> getServices() {
        return services;
    }
}
