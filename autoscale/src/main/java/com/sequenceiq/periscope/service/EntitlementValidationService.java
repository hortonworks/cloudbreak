package com.sequenceiq.periscope.service;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class EntitlementValidationService {

    @Value("${periscope.entitlementCheckEnabled:true}")
    private Boolean entitlementCheckEnabled;

    @Inject
    private EntitlementService entitlementService;

    @Cacheable(cacheNames = "accountEntitlementCache", key = "{#accountId,#cloudPlatform}")
    public boolean autoscalingEntitlementEnabled(String accountId, String cloudPlatform) {
        boolean entitled = false;
        if (Boolean.FALSE.equals(entitlementCheckEnabled) || "YARN".equalsIgnoreCase(cloudPlatform)) {
            entitled = true;
        } else if ("AWS".equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.awsAutoScalingEnabled(accountId);
        } else if ("AZURE".equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.azureAutoScalingEnabled(accountId);
        } else if ("GCP".equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.gcpAutoScalingEnabled(accountId);
        }
        return entitled;
    }

    public boolean scalingStepEntitlementEnabled(String accountId) {
        return entitlementService.dataHubScalingStepSizeEnabled(accountId);
    }
}
