package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.CloudGatewayJson;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformGatewaysV4Response implements JsonEntity {

    private Map<String, Set<CloudGatewayJson>> gateways = new HashMap<>();

    public PlatformGatewaysV4Response() {
    }

    public PlatformGatewaysV4Response(Map<String, Set<CloudGatewayJson>> gateways) {
        this.gateways = gateways;
    }

    public Map<String, Set<CloudGatewayJson>> getGateways() {
        return gateways;
    }

    public void setGateways(Map<String, Set<CloudGatewayJson>> gateways) {
        this.gateways = gateways;
    }
}
