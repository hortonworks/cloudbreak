package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.it.IntegrationTestContext;

public class ClusterGateway extends Entity {

    public static final String GATEWAY_REQUEST = "GATEWAY_REQUEST";

    private GatewayJson request;

    ClusterGateway(String newId) {
        super(newId);
        request = new GatewayJson();
    }

    ClusterGateway() {
        this(GATEWAY_REQUEST);
    }

    public ClusterGateway withTopology(GatewayTopology topology) {
        request.setTopologies(Collections.singletonList(topology.getRequest()));
        return this;
    }

    public ClusterGateway withSsoType(SSOType ssoType) {
        request.setSsoType(ssoType);
        return this;
    }

    public ClusterGateway withSsoProvider(String ssoProvider) {
        request.setSsoProvider(ssoProvider);
        return this;
    }

    public ClusterGateway withPath(String path) {
        request.setPath(path);
        return this;
    }

    public void setRequest(GatewayJson request) {
        this.request = request;
    }

    public GatewayJson getRequest() {
        return request;
    }

    public static ClusterGateway request(String key) {
        return new ClusterGateway(key);
    }

    public static ClusterGateway request() {
        return new ClusterGateway();
    }

    public static Function<IntegrationTestContext, ClusterGateway> getTestContextGateway(String key) {
        return testContext -> testContext.getContextParam(key, ClusterGateway.class);
    }

    public static Function<IntegrationTestContext, ClusterGateway> getTestContextGateway() {
        return getTestContextGateway(GATEWAY_REQUEST);
    }
}
