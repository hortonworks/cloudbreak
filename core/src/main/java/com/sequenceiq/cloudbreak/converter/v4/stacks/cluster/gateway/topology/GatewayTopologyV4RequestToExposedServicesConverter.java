package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;

@Component
public class GatewayTopologyV4RequestToExposedServicesConverter extends AbstractConversionServiceAwareConverter<GatewayTopologyV4Request, ExposedServices> {

    @Inject
    private ExposedServiceListValidator exposedServiceListValidator;

    @Override
    public ExposedServices convert(GatewayTopologyV4Request source) {
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
