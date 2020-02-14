package com.sequenceiq.freeipa.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class AttachChildEnvironmentRequestValidator implements Validator<AttachChildEnvironmentRequest> {

    static final String PARENT_ENVIRONMENT_SHOULD_HAVE_A_STACK = "Parent environment should have a stack.";

    static final String CHILD_ENVIRONMENT_ALREADY_HAS_A_STACK = "Child environment already has a stack.";

    static final String PARENT_ENVIRONMENT_CAN_NOT_BE_A_CHILD_ENVIRONMENT = "Parent environment can not be a child environment.";

    static final String CHILD_ENVIRONMENT_ALREADY_HAS_A_PARENT_ENVIRONMENT = "Child environment already has a parent environment.";

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    @Override
    public ValidationResult validate(AttachChildEnvironmentRequest subject) {
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();

        String accountId = crnService.getCurrentAccountId();
        if (stackService.findAllByEnvironmentCrnAndAccountId(subject.getParentEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error(PARENT_ENVIRONMENT_SHOULD_HAVE_A_STACK);
        }
        if (!stackService.findAllByEnvironmentCrnAndAccountId(subject.getChildEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error(CHILD_ENVIRONMENT_ALREADY_HAS_A_STACK);
        }
        if (childEnvironmentService.isChildEnvironment(subject.getParentEnvironmentCrn(), accountId)) {
            validationBuilder.error(PARENT_ENVIRONMENT_CAN_NOT_BE_A_CHILD_ENVIRONMENT);
        }
        if (childEnvironmentService.isChildEnvironment(subject.getChildEnvironmentCrn(), accountId)) {
            validationBuilder.error(CHILD_ENVIRONMENT_ALREADY_HAS_A_PARENT_ENVIRONMENT);
        }

        return validationBuilder.build();
    }
}
