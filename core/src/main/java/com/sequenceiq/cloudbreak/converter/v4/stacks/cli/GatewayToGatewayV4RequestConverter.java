package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayToGatewayV4RequestConverter {

    @Inject
    private GatewayTopologyToGatewayTopologyV4RequestConverter gatewayTopologyToGatewayTopologyV4RequestConverter;

    public GatewayV4Request convert(Gateway gateway) {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        gatewayJson.setPath(gateway.getPath());
        gatewayJson.setTokenCert(gateway.getTokenCert());
        gatewayJson.setSsoProvider(gateway.getSsoProvider());
        gatewayJson.setSsoType(gateway.getSsoType());
        gatewayJson.setGatewayType(gateway.getGatewayType());
        gatewayJson.setTopologies(convertTopologies(gateway, gatewayJson));
        return gatewayJson;
    }

    private List<GatewayTopologyV4Request> convertTopologies(Gateway gateway, GatewayV4Request gatewayJson) {
        Set<GatewayTopology> gatewayTopologies = gateway.getTopologies();
        if (!CollectionUtils.isEmpty(gatewayTopologies)) {
            return gatewayTopologies.stream()
                    .map(t -> gatewayTopologyToGatewayTopologyV4RequestConverter.convert(t))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
