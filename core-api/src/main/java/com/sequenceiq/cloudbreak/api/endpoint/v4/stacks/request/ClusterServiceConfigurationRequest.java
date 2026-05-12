package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterServiceConfigurationRequest implements JsonEntity {

    @NotNull
    @Schema(description = "Name of the service", required = true)
    private String serviceName;

    @NotNull
    @Schema(description = "Name of the service configuration parameter", required = true)
    private String configName;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @Override
    public String toString() {
        return "ClusterServiceConfigurationRequest{" +
                "serviceName='" + serviceName + '\'' +
                ", configName='" + configName + '\'' +
                '}';
    }
}
