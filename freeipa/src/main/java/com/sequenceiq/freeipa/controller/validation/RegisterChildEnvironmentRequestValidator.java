package com.sequenceiq.freeipa.controller.validation;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.registerchildenv.RegisterChildEnvironmentRequest;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class RegisterChildEnvironmentRequestValidator implements Validator<RegisterChildEnvironmentRequest> {

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    @Override
    public ValidationResult validate(RegisterChildEnvironmentRequest subject) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        String accountId = crnService.getCurrentAccountId();
        if (stackService.findAllByEnvironmentCrnAndAccountId(subject.getParentEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error("Parent environment should have a stack.");
        }
        if (!stackService.findAllByEnvironmentCrnAndAccountId(subject.getChildEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error("Child environment already has a stack.");
        }
        if (childEnvironmentService.isChildEnvironment(subject.getParentEnvironmentCrn(), accountId)) {
            validationBuilder.error("Parent environment can not be a child environment.");
        }
        if (childEnvironmentService.isChildEnvironment(subject.getChildEnvironmentCrn(), accountId)) {
            validationBuilder.error("Child environment already has a parent environment.");
        }

        return validationBuilder.build();
    }
}
