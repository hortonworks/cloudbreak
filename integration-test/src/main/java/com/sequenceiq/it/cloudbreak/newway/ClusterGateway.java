package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;
import java.util.function.Function;

import com.amazonaws.services.apigateway.model.GatewayResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

public class ClusterGateway extends AbstractCloudbreakTestDto<GatewayV4Request, GatewayResponse, ClusterGateway> {

    public static final String GATEWAY_REQUEST = "GATEWAY_REQUEST";

    ClusterGateway(String newId) {
        super(newId);
        setRequest(new GatewayV4Request());
    }

    ClusterGateway() {
        this(GATEWAY_REQUEST);
    }

    public ClusterGateway(TestContext testContext) {
        super(new GatewayV4Request(), testContext);
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
