package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayTopologyToGatewayTopologyV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTopologyToGatewayTopologyV4RequestConverter.class);

    public GatewayTopologyV4Request convert(GatewayTopology gatewayTopology) {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(gatewayTopology.getTopologyName());
        Json exposedJson = gatewayTopology.getExposedServices();
        if (exposedJson != null && StringUtils.isNotEmpty(exposedJson.getValue())) {
            try {
                gatewayTopologyJson.setExposedServices(exposedJson.get(ExposedServices.class).getServices());
            } catch (IOException e) {
                LOGGER.info("Failed to add exposedServices to response", e);
                throw new CloudbreakServiceException("Failed to add exposedServices to response", e);
            }
        }

        return gatewayTopologyJson;
    }
}
