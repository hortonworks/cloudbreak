package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;


import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlueprintServicesV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<SupportedServiceV4Response> services = new TreeSet<>();

    public Set<SupportedServiceV4Response> getServices() {
        return services;
    }

    public void setServices(Set<SupportedServiceV4Response> services) {
        this.services = services;
    }
}
