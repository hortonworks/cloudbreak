package com.sequenceiq.datalake.service.sdx;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;

@Service
public class GatewayManifester {

    @Value("${sdx.gateway.ssotype}")
    private SSOType defaultSsoType;

    public StackV4Request configureGatewayForSdxCluster(StackV4Request stackV4Request) {
        if (stackV4Request.getCluster().getGateway() == null) {
            GatewayV4Request gatewayV4Request = new GatewayV4Request();

            gatewayV4Request.setTopologies(List.of(getGatewayTopologyV4Request()));

            gatewayV4Request.setSsoType(defaultSsoType);

            stackV4Request.getCluster().setGateway(gatewayV4Request);
        }
        return stackV4Request;
    }

    private GatewayTopologyV4Request getGatewayTopologyV4Request() {
        GatewayTopologyV4Request gatewayTopologyV4Request = new GatewayTopologyV4Request();
        gatewayTopologyV4Request.setTopologyName("dp-proxy");
        gatewayTopologyV4Request.setExposedServices(List.of("ALL"));
        return gatewayTopologyV4Request;
    }

}
