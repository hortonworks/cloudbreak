package com.sequenceiq.periscope.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class EntitlementValidationService {

    @Value("${periscope.entitlementCheckEnabled:true}")
    private Boolean entitlementCheckEnabled;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ImpalaValidator impalaValidator;

    @Cacheable(cacheNames = "accountEntitlementCache", key = "{#accountId,#cloudPlatform}")
    public boolean autoscalingEntitlementEnabled(String accountId, String cloudPlatform) {
        boolean entitled = false;
        if (Boolean.FALSE.equals(entitlementCheckEnabled) || "YARN".equalsIgnoreCase(cloudPlatform)) {
            entitled = true;
        } else if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.awsAutoScalingEnabled(accountId);
        } else if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.azureAutoScalingEnabled(accountId);
        } else if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.gcpAutoScalingEnabled(accountId);
        }
        return entitled;
    }

    @Cacheable(cacheNames = "accountEntitlementCacheStopStart", key = "{#accountId,#cloudPlatform}")
    public boolean stopStartAutoscalingEntitlementEnabled(String accountId, String cloudPlatform) {
        boolean entitled = autoscalingEntitlementEnabled(accountId, cloudPlatform);
        if (!entitled) {
            return false;
        }
        if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.awsStopStartScalingEnabled(accountId);
        } else if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.azureStopStartScalingEnabled(accountId);
        } else if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.gcpStopStartScalingEnabled(accountId);
        }
        return entitled;
    }

    public List<String> impalaScheduleBasedScalingMissingEntitlements(String accountId, CloudPlatform cloudPlatform) {
        List<String> entitlements = entitlementService.getEntitlements(accountId);
        Set<Entitlement> requiredEntitlements = impalaValidator.getImpalaScheduleScalingEntitlements(cloudPlatform);
        return requiredEntitlements.stream()
                .map(Enum::name)
                .filter(name -> !entitlements.contains(name))
                .collect(Collectors.toList());
    }
}
