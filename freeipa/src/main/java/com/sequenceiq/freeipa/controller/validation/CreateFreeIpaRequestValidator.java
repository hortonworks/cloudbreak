package com.sequenceiq.freeipa.controller.validation;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class CreateFreeIpaRequestValidator implements Validator<CreateFreeIpaRequest> {

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Value("${freeipa.max.instances}")
    private int maxInstances;

    @Value("${freeipa.max.instance.groups}")
    private int maxInstanceGroups;

    @Override
    public ValidationResult validate(CreateFreeIpaRequest subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups())) {
            validationBuilder.error("FreeIPA request must contain at least one instance group.");
        } else {
            int nodesPerInstanceGroup = subject.getInstanceGroups().get(0).getNodeCount();
            if (subject.getInstanceGroups().stream().filter(ig -> ig.getNodeCount() != nodesPerInstanceGroup || ig.getNodeCount() < 1).count() > 0) {
                validationBuilder.error("All instance groups in the FreeIPA request must contain the same number of nodes per instance group " +
                        "and there must be at least 1 instance per instance group.");
            }
            if (nodesPerInstanceGroup * subject.getInstanceGroups().size() > maxInstances) {
                validationBuilder.error(String.format("FreeIPA request must contain at most %d instances.", maxInstances));
            }
            if (subject.getInstanceGroups().size() > maxInstanceGroups) {
                validationBuilder.error(String.format("FreeIPA request must contain at most %d instance groups.", maxInstanceGroups));
            }
        }

        String accountId = crnService.getCurrentAccountId();
        if (!stackService.findAllByEnvironmentCrnAndAccountId(subject.getEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error("FreeIPA already exists in environment");
        }

        return validationBuilder.build();
    }
}
