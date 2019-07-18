package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.Validator;

@Component
public class GatewayTopologyV4RequestValidator implements Validator<GatewayTopologyV4Request> {

    private final ExposedServiceListValidator exposedServiceListValidator;

    public GatewayTopologyV4RequestValidator(ExposedServiceListValidator exposedServiceListValidator) {
        this.exposedServiceListValidator = exposedServiceListValidator;
    }

    @Override
    public ValidationResult validate(GatewayTopologyV4Request subject) {
        ValidationResult topologyNameResult = ValidationResult.builder()
                .ifError(() -> StringUtils.isBlank(subject.getTopologyName()), "topologyName must be set in gateway topology.")
                .build();

        return Optional.ofNullable(subject.getExposedServices())
                .map(exposedServiceListValidator::validate)
                .map(vr -> vr.merge(topologyNameResult))
                .orElse(topologyNameResult);
    }
}
