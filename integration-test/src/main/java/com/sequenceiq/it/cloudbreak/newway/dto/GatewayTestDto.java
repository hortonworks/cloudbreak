package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class GatewayTestDto extends AbstractCloudbreakTestDto<GatewayV4Request, GatewayV4Response, GatewayTestDto> {
    public static final String NETWORK = "NETWORK";

    public GatewayTestDto(TestContext testContext) {
        super(new GatewayV4Request(), testContext);
    }

    public GatewayTestDto valid() {
        return this;
    }

    public GatewayTestDto withTopologies(String... keys) {
        getRequest().setTopologies(Stream.of(keys).map(key -> {
            GatewayTopologyTestDto gatewayEntity = getTestContext().get(key);
            return gatewayEntity.getRequest();
        }).collect(Collectors.toList()));
        return this;
    }
}
