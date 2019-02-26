package com.sequenceiq.cloudbreak.service.cluster.gateway;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.UpdateGatewayTopologiesJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

class UpdateGatewayTopologiesJsonValidator implements Validator<UpdateGatewayTopologiesJson> {

    private final Validator<GatewayTopologyJson> gatewayTopologyJsonValidator;

    private final long requestedStackId;

    private final Stack stack;

    private final AtomicBoolean executed = new AtomicBoolean(false);

    UpdateGatewayTopologiesJsonValidator(Validator<GatewayTopologyJson> gatewayTopologyJsonValidator, long requestedStackId, Stack stack) {
        this.gatewayTopologyJsonValidator = gatewayTopologyJsonValidator;
        this.requestedStackId = requestedStackId;
        this.stack = stack;
    }

    @Override
    public ValidationResult validate(UpdateGatewayTopologiesJson subject) {
        if (executed.compareAndSet(false, true)) {
            ValidationResultBuilder resultBuilder = ValidationResult.builder();
            if (isRequestEmpty(subject)) {
                return resultBuilder.error("Request is empty.").build();
            }
            validateRequestForStack(subject, resultBuilder);
            return resultBuilder.build();
        } else {
            throw new IllegalStateException("Validator was already executed!");
        }
    }

    private boolean isRequestEmpty(UpdateGatewayTopologiesJson subject) {
        return subject == null || CollectionUtils.isEmpty(subject.getTopologies());
    }

    private void validateRequestForStack(UpdateGatewayTopologiesJson subject, ValidationResultBuilder resultBuilder) {
        if (stack == null) {
            resultBuilder.error(String.format("Stack with id '%s' does not exist.", requestedStackId));
        } else {
            Cluster cluster = stack.getCluster();
            if (cluster == null) {
                resultBuilder.error(String.format("Stack '%s' does not have a cluster.", stack.getName()));
            } else {
                Gateway gateway = cluster.getGateway();
                if (gateway == null) {
                    resultBuilder.error(String.format("Cluster '%s' does not have a gateway.", cluster.getName()));
                } else {
                    validateTopologies(subject, gateway, resultBuilder);
                }
            }
        }
    }

    private void validateTopologies(UpdateGatewayTopologiesJson subject, Gateway gateway, ValidationResultBuilder resultBuilder) {
        Set<GatewayTopology> gatewayTopologies = gateway.getTopologies();
        List<GatewayTopologyJson> requestedTopologies = subject.getTopologies();
        Set<String> existingTopologyNames = gatewayTopologies.stream()
                .map(GatewayTopology::getTopologyName)
                .collect(Collectors.toSet());
        List<String> requestedTopologyNames = requestedTopologies.stream()
                .map(GatewayTopologyJson::getTopologyName)
                .collect(Collectors.toList());

        existingTopologyNames.forEach(requestedTopologyNames::remove);
        resultBuilder.ifError(() -> !requestedTopologyNames.isEmpty(),
                String.format("No such topology in stack %s : %s", stack.getName(), joinMissingTopologyNames(requestedTopologyNames)));

        requestedTopologies.stream()
                .map(gatewayTopologyJsonValidator::validate)
                .forEach(resultBuilder::merge);
    }

    private String joinMissingTopologyNames(List<String> requestedTopologyNames) {
        return String.join(" ,", requestedTopologyNames);
    }
}
