package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GatewayEntity extends AbstractCloudbreakEntity<GatewayJson, GatewayJson, GatewayEntity> {
    public static final String NETWORK = "NETWORK";

    public GatewayEntity(TestContext testContext) {
        super(new GatewayJson(), testContext);
    }

    public GatewayEntity valid() {
        return this;
    }

    public GatewayEntity withTopologies(String... keys) {
        getRequest().setTopologies(Stream.of(keys).map(key -> {
            GatewayTopologyEntity gatewayEntity = getTestContext().get(key);
            return gatewayEntity.getRequest();
        }).collect(Collectors.toList()));
        return this;
    }
}
