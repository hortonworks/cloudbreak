package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportedServicesV4Response implements JsonEntity {

    private Set<SupportedServiceV4Response> services = new HashSet<>();

    public Set<SupportedServiceV4Response> getServices() {
        return services;
    }

    public void setServices(Set<SupportedServiceV4Response> services) {
        this.services = services;
    }
}
