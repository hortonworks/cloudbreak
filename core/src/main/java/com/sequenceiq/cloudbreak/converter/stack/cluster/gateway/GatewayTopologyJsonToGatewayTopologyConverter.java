package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayTopologyJsonToGatewayTopologyConverter extends AbstractConversionServiceAwareConverter<GatewayTopologyJson, GatewayTopology> {
    @Override
    public GatewayTopology convert(GatewayTopologyJson source) {
        return null;
    }
}
