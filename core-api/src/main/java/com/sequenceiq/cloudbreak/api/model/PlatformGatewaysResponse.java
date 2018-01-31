package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformGatewaysResponse implements JsonEntity {

    private Map<String, Set<CloudGatewayJson>> gateways = new HashMap<>();

    public PlatformGatewaysResponse() {
    }

    public PlatformGatewaysResponse(Map<String, Set<CloudGatewayJson>> gateways) {
        this.gateways = gateways;
    }

    public Map<String, Set<CloudGatewayJson>> getGateways() {
        return gateways;
    }

    public void setGateways(Map<String, Set<CloudGatewayJson>> gateways) {
        this.gateways = gateways;
    }
}
