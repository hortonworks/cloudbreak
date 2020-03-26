package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformGatewaysV4Response implements JsonEntity {

    private Map<String, Set<CloudGatewayV4Request>> gateways = new HashMap<>();

    public PlatformGatewaysV4Response() {
    }

    public PlatformGatewaysV4Response(Map<String, Set<CloudGatewayV4Request>> gateways) {
        this.gateways = gateways;
    }

    public Map<String, Set<CloudGatewayV4Request>> getGateways() {
        return gateways;
    }

    public void setGateways(Map<String, Set<CloudGatewayV4Request>> gateways) {
        this.gateways = gateways;
    }
}
