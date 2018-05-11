package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@Component
public class GatewayJsonToGatewayConverter extends AbstractConversionServiceAwareConverter<GatewayJson, Gateway> {

    @Override
    public Gateway convert(GatewayJson source) {

        return null;
    }
}
