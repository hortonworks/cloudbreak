package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.ArrayList;
import java.util.List;

public class ExposedServices {

    private List<String> services;

    public ExposedServices() {
        services = new ArrayList<>();
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
