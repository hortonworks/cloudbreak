package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GatewayTopologyEntity extends AbstractCloudbreakEntity<GatewayTopologyJson, GatewayTopologyJson, GatewayTopologyEntity> {
    public static final String NETWORK = "NETWORK";

    public GatewayTopologyEntity(TestContext testContext) {
        super(new GatewayTopologyJson(), testContext);
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
