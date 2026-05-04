package com.sequenceiq.environment.environment.validation.validators;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class EnvironmentComputeClusterEntitlementValidator {

    @Inject
    private EntitlementService entitlementService;

    public ValidationResult validate(String accountId) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (!entitlementService.isComputeClusterEnabled(accountId)) {
            resultBuilder.error("You are not entitled to use externalized compute cluster. "
                    + "Please contact Cloudera to enable ENABLE_COMPUTE_CLUSTER for your account.");
        }
        return resultBuilder.build();
    }
}
