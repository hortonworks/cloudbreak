package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class GatewayTopologyV4RequestToGatewayTopologyConverter {

    @Inject
    private GatewayTopologyV4RequestValidator validator;

    @Inject
    private GatewayTopologyV4RequestToExposedServicesConverter gatewayTopologyV4RequestToExposedServicesConverter;

    public GatewayTopology convert(GatewayTopologyV4Request source) {
        ValidationResult validationResult = validator.validate(source);
        if (validationResult.getState() == State.ERROR) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(source.getTopologyName());
        convertExposedServices(gatewayTopology, source);
        return gatewayTopology;
    }

    private void convertExposedServices(GatewayTopology gatewayTopology, GatewayTopologyV4Request source) {
        try {
            if (!CollectionUtils.isEmpty(source.getExposedServices())) {
                ExposedServices exposedServices = gatewayTopologyV4RequestToExposedServicesConverter.convert(source);
                gatewayTopology.setExposedServices(new Json(exposedServices));
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid exposedServices in request. Could not be parsed to JSON.", e);
        }
    }
}
