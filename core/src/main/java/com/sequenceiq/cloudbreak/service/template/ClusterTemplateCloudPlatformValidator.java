package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class ClusterTemplateCloudPlatformValidator {

    private final Set<String> enabledPlatforms;

    private final EntitlementService entitlementService;

    public ClusterTemplateCloudPlatformValidator(@Value("${cb.enabledplatforms:}") Set<String> enabledPlatforms, EntitlementService entitlementService) {
        this.enabledPlatforms = enabledPlatforms;
        this.entitlementService = entitlementService;
    }

    public boolean isClusterTemplateCloudPlatformValid(String cloudPlatform, String accountId) {
        return (enabledPlatforms.contains(cloudPlatform) || CollectionUtils.isEmpty(enabledPlatforms))
                && (!AZURE.name().equalsIgnoreCase(cloudPlatform) || entitlementService.azureEnabled(INTERNAL_ACTOR_CRN, accountId));
    }

}
