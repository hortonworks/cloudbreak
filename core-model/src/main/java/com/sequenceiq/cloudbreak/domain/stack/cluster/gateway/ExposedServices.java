package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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

    public void setServices(List<String> services) {
        this.services = services;
    }
}
