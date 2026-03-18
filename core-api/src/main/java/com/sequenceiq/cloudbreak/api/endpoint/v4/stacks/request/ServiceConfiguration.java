package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceConfiguration implements JsonEntity {

    @NotNull
    @Schema(description = "Name of the service to update", required = true)
    private String serviceName;

    @NotNull
    @Schema(description = "Name of the service configuration parameter to update", required = true)
    private String configName;

    @NotNull
    @Schema(description = "Configuration value", required = true)
    private String value;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ServiceConfiguration{" +
                "serviceName='" + serviceName + '\'' +
                ", configName='" + configName + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
