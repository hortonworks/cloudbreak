package com.sequenceiq.cloudbreak.controller.validation.datalake;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class DataLakeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeValidator.class);

    @Inject
    private StackService stackService;

    public void validate(Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (stack.getEnvironmentCrn() != null && StackType.DATALAKE.equals(stack.getType())) {
            LOGGER.info("Get datalake count in environment {}", stack.getEnvironmentCrn());
            Long datalakesInEnv = stackService.countByEnvironmentCrnAndStackType(stack.getEnvironmentCrn(), StackType.DATALAKE);
            if (datalakesInEnv > 1L) {
                validationBuilder.error("Only 1 Data Lake / Environment is allowed.");
            }
        }
    }

}
