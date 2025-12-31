package com.sequenceiq.distrox.v1.support.controller;

import static com.sequenceiq.common.model.Architecture.ARM64;
import static com.sequenceiq.common.model.Architecture.X86_64;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.support.SupportV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.support.response.DataHubPlatformSupportRequirements;
import com.sequenceiq.cloudbreak.controller.v4.NotificationController;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;

@Controller
public class SupportV1Controller extends NotificationController implements SupportV1Endpoint {

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public DataHubPlatformSupportRequirements getInstanceTypesByPlatform(String cloudPlatform) {
        DataHubPlatformSupportRequirements clusterTemplateRequirements = new DataHubPlatformSupportRequirements();
        if (StringUtils.isNotBlank(cloudPlatform)) {
            clusterTemplateRequirements.setDefaultX86InstanceTypeRequirements(
                    clusterTemplateService.getDefaultInstanceTypesFromTemplates(cloudPlatform, X86_64));
            clusterTemplateRequirements.setDefaultArmInstanceTypeRequirements(
                    clusterTemplateService.getDefaultInstanceTypesFromTemplates(cloudPlatform, ARM64));
        }
        return clusterTemplateRequirements;
    }
}
