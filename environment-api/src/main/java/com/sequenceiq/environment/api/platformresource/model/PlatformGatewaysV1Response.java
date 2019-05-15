package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformGatewaysV1Response implements Serializable {

    private Map<String, Set<CloudGatewayV1Request>> gateways = new HashMap<>();

    public PlatformGatewaysV1Response() {
    }

    public PlatformGatewaysV1Response(Map<String, Set<CloudGatewayV1Request>> gateways) {
        this.gateways = gateways;
    }

    public Map<String, Set<CloudGatewayV1Request>> getGateways() {
        return gateways;
    }

    public void setGateways(Map<String, Set<CloudGatewayV1Request>> gateways) {
        this.gateways = gateways;
    }
}
