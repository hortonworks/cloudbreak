package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayTopologyToGatewayTopologyJsonConverter extends AbstractConversionServiceAwareConverter<GatewayTopology, GatewayTopologyJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTopologyToGatewayTopologyJsonConverter.class);

    @Override
    public GatewayTopologyJson convert(GatewayTopology gatewayTopology) {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName(gatewayTopology.getTopologyName());
        Json exposedJson = gatewayTopology.getExposedServices();
        if (exposedJson != null && StringUtils.isNoneEmpty(exposedJson.getValue())) {
            try {
                gatewayTopologyJson.setExposedServices(exposedJson.get(ExposedServices.class).getServices());
            } catch (IOException e) {
                LOGGER.error("Failed to add exposedServices to response", e);
                throw new CloudbreakApiException("Failed to add exposedServices to response", e);
            }
        }

        return gatewayTopologyJson;
    }
}
