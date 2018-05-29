package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        if (shouldCheckTopologyNames(subject)) {

            List<String> topologyNames = subject.getTopologies().stream()
                    .map(GatewayTopologyJson::getTopologyName)
                    .collect(Collectors.toList());

            Set<String> uniqueNames = new HashSet<>(topologyNames);
            uniqueNames.stream().forEach(topologyNames::remove);

            return validationResultBuilder
                    .ifError(() -> !topologyNames.isEmpty(),
                            "There are duplicate topology names is gateway.topologies! "
                            + "All topology name should be unique for a gateway. Duplicates are: "
                            + topologyNames.stream().collect(Collectors.joining(", ")))
                    .build();
        }
        return validationResultBuilder.build();
    }

    private boolean shouldCheckTopologyNames(GatewayJson subject) {
        return !gatewayConvertUtil.isLegacyGatewayRequest(subject) && !CollectionUtils.isEmpty(subject.getTopologies());
    }
}
