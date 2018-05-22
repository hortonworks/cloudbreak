package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class GatewayTopologyJsonValidator implements Validator<GatewayTopologyJson> {

    private final ExposedServiceListValidator exposedServiceListValidator;

    public GatewayTopologyJsonValidator(ExposedServiceListValidator exposedServiceListValidator) {
        this.exposedServiceListValidator = exposedServiceListValidator;
    }

    @Override
    public ValidationResult validate(GatewayTopologyJson target) {
        ValidationResult topologyNameResult = ValidationResult.builder()
                .ifError(() -> StringUtils.isBlank(target.getTopologyName()), "topologyName must be set in gateway topology.")
                .build();

        return Optional.ofNullable(target.getExposedServices())
                .map(exposedServiceListValidator::validate)
                .map(vr -> vr.merge(topologyNameResult))
                .orElse(topologyNameResult);
    }
}
