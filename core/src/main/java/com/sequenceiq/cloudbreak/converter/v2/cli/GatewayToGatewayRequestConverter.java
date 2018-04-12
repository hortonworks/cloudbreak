package com.sequenceiq.cloudbreak.converter.v2.cli;

import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GatewayToGatewayRequestConverter extends AbstractConversionServiceAwareConverter<Gateway, GatewayJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayToGatewayRequestConverter.class);

    @Override
    public GatewayJson convert(Gateway source) {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setEnableGateway(source.getEnableGateway());
        try {
            gatewayJson.setExposedServices(source.getExposedServices().get(List.class));
        } catch (IOException e) {
            gatewayJson.setExposedServices(new ArrayList<>());
        }
        gatewayJson.setGatewayType(source.getGatewayType());
        gatewayJson.setPath(source.getPath());
        gatewayJson.setSsoProvider(source.getSsoProvider());
        gatewayJson.setSsoType(source.getSsoType());
        gatewayJson.setTokenCert(source.getTokenCert());
        gatewayJson.setTopologyName(source.getTopologyName());
        return gatewayJson;
    }

}
