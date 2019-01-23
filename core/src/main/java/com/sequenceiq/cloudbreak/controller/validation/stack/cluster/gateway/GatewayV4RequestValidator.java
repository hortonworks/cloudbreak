package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;

@Component
public class GatewayV4RequestValidator implements Validator<GatewayV4Request> {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Override
    public ValidationResult validate(GatewayV4Request subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getTopologies())) {
            return validationResultBuilder.error("No topology is defined in gateway request. Please define a topology in "
                    + "'gateway.topologies'.").build();
        }
        validateTopologyNames(subject, validationResultBuilder);
        return validationResultBuilder.build();
    }

    private void validateTopologyNames(GatewayV4Request subject, ValidationResultBuilder validationResultBuilder) {
        List<String> topologyNames = subject.getTopologies().stream()
                .map(GatewayTopologyV4Request::getTopologyName)
                .collect(Collectors.toList());
        Set<String> duplicates = topologyNames.stream()
                .filter(i -> Collections.frequency(topologyNames, i) > 1)
                .collect(Collectors.toSet());

        validationResultBuilder.ifError(() -> !duplicates.isEmpty(),
                "There are duplicate topology names is gateway.topologies! "
                        + "All topology name should be unique for a gateway. Duplicates are: "
                        + String.join(", ", duplicates));
    }
}
