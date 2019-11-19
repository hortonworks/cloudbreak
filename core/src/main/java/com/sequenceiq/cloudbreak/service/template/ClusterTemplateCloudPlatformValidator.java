package com.sequenceiq.cloudbreak.service.template;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;

@Component
public class ClusterTemplateCloudPlatformValidator {

    @VisibleForTesting
    static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private final Set<String> enabledPlatforms;

    private final EntitlementService entitlementService;

    public ClusterTemplateCloudPlatformValidator(@Value("${cb.enabledplatforms:}") Set<String> enabledPlatforms, EntitlementService entitlementService) {
        this.enabledPlatforms = enabledPlatforms;
        this.entitlementService = entitlementService;
    }

    public boolean isClusterTemplateCloudPlatformValid(String cloudPlatform, String accountId) {
        return (enabledPlatforms.contains(cloudPlatform) || CollectionUtils.isEmpty(enabledPlatforms))
                && (!AZURE.name().equalsIgnoreCase(cloudPlatform) || entitlementService.azureEnabled(IAM_INTERNAL_ACTOR_CRN, accountId));
    }

}
