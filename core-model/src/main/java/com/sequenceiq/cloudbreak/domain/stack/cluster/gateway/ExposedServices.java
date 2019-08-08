package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExposedServices {

    private List<String> services;

    public ExposedServices() {
        services = new ArrayList<>();
    }

    /**
     * List the service list from the database. Possible values: ALL or any services are provided by user
     * @return unmodified list based on the GatewayTopology.exposedServices field
     */
    public List<String> getServices() {
        return services;
    }

    /**
     * List of the modified list based on {@code getServices()}.
     * @return If the {@code getServices()} contains ALL, the method will expands it to {@code ExposedService.getAllKnoxExposed()}.
     * Any other case, it will return with {@code getServices()}
     */
    @JsonIgnore
    public List<String> getFullServiceList() {
        if (services.contains(ExposedService.ALL.name())) {
            return ExposedService.getAllKnoxExposed();
        }
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
