package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayTopologyToGatewayTopologyV4ResponseConverter extends AbstractConversionServiceAwareConverter<GatewayTopology, GatewayTopologyV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayTopologyToGatewayTopologyV4ResponseConverter.class);

    @Override
    public GatewayTopologyV4Response convert(GatewayTopology source) {
        GatewayTopologyV4Response response = new GatewayTopologyV4Response();
        ExposedServices exposedServices = getExposedServices(source);
        if (exposedServices != null) {
            response.setExposedServices(exposedServices.getServices());
        }
        response.setTopologyName(source.getTopologyName());
        return null;
    }

    private ExposedServices getExposedServices(GatewayTopology source) {
        try {
            return source.getExposedServices().get(ExposedServices.class);
        } catch (IOException e) {
            LOGGER.info("Could not extract expose services", e);
        }
        return null;
    }
}
