package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyJsonValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@Component
public class GatewayTopologyJsonToGatewayTopologyConverter extends AbstractConversionServiceAwareConverter<GatewayTopologyJson, GatewayTopology> {

    @Inject
    private GatewayTopologyJsonValidator validator;

    @Override
    public GatewayTopology convert(GatewayTopologyJson source) {
        ValidationResult validationResult = validator.validate(source);
        if (validationResult.getState() == State.ERROR) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(source.getTopologyName());
        convertExposedServices(gatewayTopology, source.getExposedServices());
        return gatewayTopology;
    }

    private void convertExposedServices(GatewayTopology gatewayTopology, List<String> exposedServiceList) {
        try {
            if (!CollectionUtils.isEmpty(exposedServiceList)) {
                ExposedServices exposedServices = new ExposedServices();
                if (exposedServiceList.contains(ExposedService.ALL.name())) {
                    exposedServices.setServices(ExposedService.getAllKnoxExposed());
                } else {
                    exposedServices.setServices(exposedServiceList);
                }
                gatewayTopology.setExposedServices(new Json(exposedServices));
            }
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid exposedServices in request. Could not be parsed to JSON.", e);
        }
    }
}
