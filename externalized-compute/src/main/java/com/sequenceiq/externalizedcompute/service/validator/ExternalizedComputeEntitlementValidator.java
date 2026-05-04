package com.sequenceiq.externalizedcompute.service.validator;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class ExternalizedComputeEntitlementValidator {

    @Inject
    private EntitlementService entitlementService;

    public void validateComputeClusterEntitlement(String accountId) {
        if (!entitlementService.isComputeClusterEnabled(accountId)) {
            throw new BadRequestException("You are not entitled to use externalized compute cluster. "
                    + "Please contact Cloudera to enable ENABLE_COMPUTE_CLUSTER for your account.");
        }
    }
}
