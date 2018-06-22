package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;

public class GatewayTopology extends Entity {

    public static final String GATEWAY_TOPOLOGY_REQUEST = "GATEWAY_TOPOLOGY_REQUEST";

    private GatewayTopologyJson request;

    GatewayTopology(String newId) {
        super(newId);
        request = new GatewayTopologyJson();
    }

    GatewayTopology() {
        this(GATEWAY_TOPOLOGY_REQUEST);
    }

    public GatewayTopologyJson getRequest() {
        return request;
    }

    public void setRequest(GatewayTopologyJson request) {
        this.request = request;
    }

    public GatewayTopology withName(String name) {
        request.setTopologyName(name);
        return this;
    }

    public GatewayTopology withExposedServices(List<String> exposedServices) {
        request.setExposedServices(exposedServices);
        return this;
    }

    public static GatewayTopology request(String key) {
        return new GatewayTopology(key);
    }

    public static GatewayTopology request() {
        return new GatewayTopology();
    }
}
