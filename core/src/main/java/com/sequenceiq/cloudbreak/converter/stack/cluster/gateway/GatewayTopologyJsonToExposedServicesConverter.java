package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;

@Component
public class GatewayTopologyJsonToExposedServicesConverter extends AbstractConversionServiceAwareConverter<GatewayTopologyJson, ExposedServices> {

    @Inject
    private ExposedServiceListValidator exposedServiceListValidator;

    @Override
    public ExposedServices convert(GatewayTopologyJson source) {
        List<String> exposedServiceList = source.getExposedServices();
        ExposedServices exposedServices = new ExposedServices();
        if (!CollectionUtils.isEmpty(exposedServiceList)) {
            ValidationResult validationResult = exposedServiceListValidator.validate(exposedServiceList);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }

            if (exposedServiceList.contains(ExposedService.ALL.name())) {
                exposedServices.setServices(ExposedService.getAllKnoxExposed());
            } else {
                exposedServices.setServices(exposedServiceList);
            }
        }
        return exposedServices;
    }
}
