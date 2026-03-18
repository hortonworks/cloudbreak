package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterServiceConfigurationRequest implements JsonEntity {

    @NotNull
    @Valid
    private List<ServiceConfiguration> serviceConfigurations = new ArrayList<>();

    public List<ServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public void setServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
    }

    @Override
    public String toString() {
        return "UpdateClusterServiceConfigurationRequest{" +
                "serviceConfigurations=" + serviceConfigurations +
                '}';
    }
}
