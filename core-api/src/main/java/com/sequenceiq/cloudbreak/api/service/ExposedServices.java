package com.sequenceiq.cloudbreak.api.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposedServices {

    private List<ExposedService> services;

    @JsonCreator
    public ExposedServices(@JsonProperty("services") List<ExposedService> services) {
        this.services = services;
    }

    public List<ExposedService> getServices() {
        return services;
    }
}
