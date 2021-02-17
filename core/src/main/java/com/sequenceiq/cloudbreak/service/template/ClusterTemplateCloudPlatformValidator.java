package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class ClusterTemplateCloudPlatformValidator {

    private final Set<String> enabledPlatforms;

    private final EntitlementService entitlementService;

    public ClusterTemplateCloudPlatformValidator(@Value("${cdp.platforms.supportedPlatforms}") Set<String> enabledPlatforms,
            EntitlementService entitlementService) {
        this.enabledPlatforms = enabledPlatforms;
        this.entitlementService = entitlementService;
    }

    public boolean isClusterTemplateCloudPlatformValid(String cloudPlatform, String accountId) {
        return isPlatformEnabled(cloudPlatform)
                && (notCloudEntitlementRequiredPlatform(cloudPlatform) || isCloudEntitlementEnabeledForTheAccount(cloudPlatform, accountId));
    }

    public boolean isPlatformEnabled(String cloudPlatform) {
        return enabledPlatforms.contains(cloudPlatform) || CollectionUtils.isEmpty(enabledPlatforms);
    }

    private boolean notCloudEntitlementRequiredPlatform(String cloudPlatform) {
        return !AZURE.name().equalsIgnoreCase(cloudPlatform) && !GCP.name().equalsIgnoreCase(cloudPlatform);
    }

    private boolean isCloudEntitlementEnabeledForTheAccount(String cloudPlatform, String accountId) {
        return (AZURE.name().equalsIgnoreCase(cloudPlatform) && entitlementService.azureEnabled(accountId))
                || (GCP.name().equalsIgnoreCase(cloudPlatform) && entitlementService.gcpEnabled(accountId));
    }

}
