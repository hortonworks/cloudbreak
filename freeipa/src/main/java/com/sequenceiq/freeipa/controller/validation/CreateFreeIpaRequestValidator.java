package com.sequenceiq.freeipa.controller.validation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class CreateFreeIpaRequestValidator implements Validator<CreateFreeIpaRequest> {

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Inject
    private CredentialService credentialService;

    @Override
    public ValidationResult validate(CreateFreeIpaRequest subject) {
        ValidationResultBuilder validationBuilder = ValidationResult.builder();
        if (CollectionUtils.isEmpty(subject.getInstanceGroups()) || subject.getInstanceGroups().size() != 1) {
            validationBuilder.error("Stack request must contain a single instance group.");
        }

        String accountId = crnService.getCurrentAccountId();
        if (!stackService.findAllByEnvironmentCrnAndAccountId(subject.getEnvironmentCrn(), accountId).isEmpty()) {
            validationBuilder.error("FreeIPA already exists in environment");
        }

        return validationBuilder.build();
    }
}
