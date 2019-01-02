package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GatewayTopologyEntity extends AbstractCloudbreakEntity<GatewayTopologyV4Request, GatewayTopologyV4Response, GatewayTopologyEntity> {
    public static final String NETWORK = "NETWORK";

    public GatewayTopologyEntity(TestContext testContext) {
        super(new GatewayTopologyV4Request(), testContext);
    }

    public GatewayTopologyEntity valid() {
        return this;
    }

    public GatewayTopologyEntity withExposedServices(String... exposedServices) {
        getRequest().setExposedServices(Arrays.asList(exposedServices));
        return this;
    }

    public GatewayTopologyEntity withTopologyName(String topologyName) {
        getRequest().setTopologyName(topologyName);
        return this;
    }
}
