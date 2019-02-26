package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;

@Component
public class GatewayJsonValidator implements Validator<GatewayJson> {

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Override
    public ValidationResult validate(GatewayJson subject) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        // when enableGateway is false and there is no topology in topologies then its a valid legacy request
        if (gatewayConvertUtil.isDisabledLegacyGateway(subject) && CollectionUtils.isEmpty(subject.getTopologies())) {
            return validationResultBuilder.build();
        }
        if (noTopologyIsDefined(subject)) {
            return validationResultBuilder.error("No topology is defined in gateway request. Please define a topology in "
                    + "'gateway.topologies'.").build();
        }
        if (shouldCheckTopologyNames(subject)) {
            validateTopologyNames(subject, validationResultBuilder);
        }
        return validationResultBuilder.build();
    }

    private boolean noTopologyIsDefined(GatewayJson subject) {
        boolean noTopologyListIsDefined = CollectionUtils.isEmpty(subject.getTopologies());
        return noDeprecatedTopologyIsDefined(subject) && noTopologyListIsDefined;
    }

    private boolean noDeprecatedTopologyIsDefined(GatewayJson subject) {
        return CollectionUtils.isEmpty(subject.getExposedServices()) && StringUtils.isEmpty(subject.getTopologyName());
    }

    private boolean shouldCheckTopologyNames(GatewayJson subject) {
        return !gatewayConvertUtil.isLegacyTopologyRequest(subject) && !CollectionUtils.isEmpty(subject.getTopologies());
    }

    private void validateTopologyNames(GatewayJson subject, ValidationResultBuilder validationResultBuilder) {
        List<String> topologyNames = subject.getTopologies().stream()
                .map(GatewayTopologyJson::getTopologyName)
                .collect(Collectors.toList());

        Set<String> uniqueNames = new HashSet<>(topologyNames);
        uniqueNames.forEach(topologyNames::remove);

        validationResultBuilder.ifError(() -> !topologyNames.isEmpty(),
                "There are duplicate topology names is gateway.topologies! "
                        + "All topology name should be unique for a gateway. Duplicates are: "
                        + String.join(", ", topologyNames));
    }
}
