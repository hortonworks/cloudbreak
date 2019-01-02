package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GatewayEntity extends AbstractCloudbreakEntity<GatewayV4Request, GatewayV4Response, GatewayEntity> {
    public static final String NETWORK = "NETWORK";

    public GatewayEntity(TestContext testContext) {
        super(new GatewayV4Request(), testContext);
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
