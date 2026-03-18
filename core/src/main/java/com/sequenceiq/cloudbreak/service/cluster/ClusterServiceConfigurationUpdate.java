package com.sequenceiq.cloudbreak.service.cluster;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ServiceConfiguration;

public class ClusterServiceConfigurationUpdate {

    private List<ServiceConfiguration> serviceConfigurations = new ArrayList<>();

    public List<ServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public void setServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
    }

    @Override
    public String toString() {
        return "ClusterServiceConfigurationUpdate{" +
                "serviceConfigurations=" + serviceConfigurations +
                '}';
    }
}
