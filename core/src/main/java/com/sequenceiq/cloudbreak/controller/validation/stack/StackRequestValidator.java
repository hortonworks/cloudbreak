package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupBase;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class StackRequestValidator implements Validator<StackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestValidator.class);

    private final Validator<TemplateRequest> templateRequestValidator;

    public StackRequestValidator(Validator<TemplateRequest> templateRequestValidator) {
        this.templateRequestValidator = templateRequestValidator;
    }

    @Override
    public ValidationResult validate(StackRequest stackRequest) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(stackRequest.getInstanceGroups())) {
            validationBuilder.error("Stack request must contain instance groups.");
        }
        validationBuilder = validateHostgroupInstanceGroupMapping(stackRequest, validationBuilder);
        validationBuilder = validateTemplates(stackRequest, validationBuilder);
        return validationBuilder.build();
    }

    private ValidationResultBuilder validateHostgroupInstanceGroupMapping(StackRequest stackRequest, ValidationResultBuilder validationBuilder) {
        Set<String> instanceGroupSet = stackRequest.getInstanceGroups()
                .stream()
                .map(InstanceGroupBase::getGroup)
                .collect(Collectors.toSet());
        Set<String> hostGroupSet = stackRequest.getClusterRequest().getHostGroups()
                .stream()
                .map(HostGroupBase::getName)
                .collect(Collectors.toSet());

        if (!instanceGroupSet.containsAll(hostGroupSet)) {
            Set<String> newHostGroupSet = Sets.newHashSet(hostGroupSet);
            newHostGroupSet.removeAll(instanceGroupSet);
            validationBuilder.error("There are host groups in the request that do not have a corresponding instance group: "
                    + newHostGroupSet.stream().collect(Collectors.joining(", ")));
        }

        if (!hostGroupSet.containsAll(instanceGroupSet)) {
            instanceGroupSet.removeAll(hostGroupSet);
            validationBuilder.error("There are instance groups in the request that do not have a corresponding host group: "
                    + instanceGroupSet.stream().collect(Collectors.joining(", ")));
        }

        return validationBuilder;
    }

    private ValidationResultBuilder validateTemplates(StackRequest stackRequest, ValidationResultBuilder resultBuilder) {
        stackRequest.getInstanceGroups()
                .stream()
                .map(i -> templateRequestValidator.validate(i.getTemplate()))
                .reduce(ValidationResult::merge)
                .ifPresent(resultBuilder::merge);
        return resultBuilder;
    }
}

