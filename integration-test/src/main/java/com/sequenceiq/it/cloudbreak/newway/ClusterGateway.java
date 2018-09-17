package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;
import java.util.function.Function;

import com.amazonaws.services.apigateway.model.GatewayResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterGateway extends AbstractCloudbreakEntity<GatewayJson, GatewayResponse, ClusterGateway> {

    public static final String GATEWAY_REQUEST = "GATEWAY_REQUEST";

    ClusterGateway(String newId) {
        super(newId);
        setRequest(new GatewayJson());
    }

    ClusterGateway() {
        this(GATEWAY_REQUEST);
    }

    public ClusterGateway(TestContext testContext) {
        super(new GatewayJson(), testContext);
    }

    public ClusterGateway withTopology(GatewayTopology topology) {
        getRequest().setTopologies(Collections.singletonList(topology.getRequest()));
        return this;
    }

    public ClusterGateway withSsoType(SSOType ssoType) {
        getRequest().setSsoType(ssoType);
        return this;
    }

    public ClusterGateway withSsoProvider(String ssoProvider) {
        getRequest().setSsoProvider(ssoProvider);
        return this;
    }

    public ClusterGateway withPath(String path) {
        getRequest().setPath(path);
        return this;
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
