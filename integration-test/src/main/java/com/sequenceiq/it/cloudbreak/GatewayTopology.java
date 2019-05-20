package com.sequenceiq.it.cloudbreak;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;

public class GatewayTopology extends Entity {

    public static final String GATEWAY_TOPOLOGY_REQUEST = "GATEWAY_TOPOLOGY_REQUEST";

    private GatewayTopologyV4Request request;

    GatewayTopology(String newId) {
        super(newId);
        request = new GatewayTopologyV4Request();
    }

    GatewayTopology() {
        this(GATEWAY_TOPOLOGY_REQUEST);
    }

    public GatewayTopologyV4Request getRequest() {
        return request;
    }

    public void setRequest(GatewayTopologyV4Request request) {
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
