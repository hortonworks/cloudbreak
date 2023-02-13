package com.sequenceiq.it.cloudbreak.dto;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class GatewayTopologyTestDto extends AbstractCloudbreakTestDto<GatewayTopologyV4Request, GatewayTopologyV4Response, GatewayTopologyTestDto> {

    public GatewayTopologyTestDto(TestContext testContext) {
        super(new GatewayTopologyV4Request(), testContext);
    }

    public GatewayTopologyTestDto valid() {
        return this;
    }

    public GatewayTopologyTestDto withExposedServices(String... exposedServices) {
        getRequest().setExposedServices(Arrays.asList(exposedServices));
        return this;
    }

    public GatewayTopologyTestDto withTopologyName(String topologyName) {
        getRequest().setTopologyName(topologyName);
        return this;
    }
}
