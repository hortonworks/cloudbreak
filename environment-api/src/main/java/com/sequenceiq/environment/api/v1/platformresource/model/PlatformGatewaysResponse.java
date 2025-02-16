package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformGatewaysResponse implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<CloudGatewayRequest>> gateways = new HashMap<>();

    public PlatformGatewaysResponse() {
    }

    public PlatformGatewaysResponse(Map<String, Set<CloudGatewayRequest>> gateways) {
        this.gateways = gateways;
    }

    public Map<String, Set<CloudGatewayRequest>> getGateways() {
        return gateways;
    }

    public void setGateways(Map<String, Set<CloudGatewayRequest>> gateways) {
        this.gateways = gateways;
    }

    @Override
    public String toString() {
        return "PlatformGatewaysResponse{" +
                "gateways=" + gateways +
                '}';
    }
}
