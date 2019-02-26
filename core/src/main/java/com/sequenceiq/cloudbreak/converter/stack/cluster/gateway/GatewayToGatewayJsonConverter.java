package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayToGatewayJsonConverter extends AbstractConversionServiceAwareConverter<Gateway, GatewayJson> {

    @Override
    public GatewayJson convert(Gateway gateway) {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setPath(gateway.getPath());
        gatewayJson.setTokenCert(gateway.getTokenCert());
        gatewayJson.setSsoProvider(gateway.getSsoProvider());
        gatewayJson.setSsoType(gateway.getSsoType());
        gatewayJson.setGatewayType(gateway.getGatewayType());
        convertTopologies(gateway, gatewayJson);
        return gatewayJson;
    }

    private void convertTopologies(Gateway gateway, GatewayJson gatewayJson) {
        Set<GatewayTopology> gatewayTopologies = gateway.getTopologies();
        if (!CollectionUtils.isEmpty(gatewayTopologies)) {
            List<GatewayTopologyJson> topologies = gatewayTopologies.stream()
                    .map(t -> getConversionService().convert(t, GatewayTopologyJson.class))
                    .collect(Collectors.toList());
            gatewayJson.setTopologies(topologies);
        }
    }
}
