package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyToGatewayTopologyV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class GatewayToGatewayV4ResponseConverter {

    @Inject
    private GatewayTopologyToGatewayTopologyV4ResponseConverter gatewayTopologyToGatewayTopologyV4ResponseConverter;

    public GatewayV4Response convert(Gateway source) {
        GatewayV4Response response = new GatewayV4Response();
        response.setGatewayType(source.getGatewayType());
        response.setPath(source.getPath());
        response.setSsoProvider(source.getSsoProvider());
        response.setSsoType(source.getSsoType());
        response.setTokenCert(source.getTokenCert());
        response.setTopologies(source.getTopologies().stream()
                .map(t -> gatewayTopologyToGatewayTopologyV4ResponseConverter.convert(t))
                .collect(Collectors.toList()));
        return response;
    }
}
