package com.sequenceiq.cloudbreak.controller.validation.datalake;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class DataLakeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeValidator.class);

    @Inject
    private SdxClientService sdxClientService;

    public void validate(Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (stack.getEnvironmentCrn() != null && StackType.DATALAKE.equals(stack.getType())) {
            LOGGER.info("Get data lake count in environment {}", stack.getEnvironmentCrn());
            Long datalakesInEnv = sdxClientService.getByEnvironmentCrn(stack.getEnvironmentCrn())
                    .stream()
                    .filter(sdxClusterResponse -> !sdxClusterResponse.isDetached())
                    .filter(sdxClusterResponse -> !sdxClusterResponse.getCrn().equals(stack.getResourceCrn()))
                    .count();
            if (datalakesInEnv >= 1L) {
                validationBuilder.error("Only 1 Data Lake / Environment is allowed.");
            }
        }
    }

}
