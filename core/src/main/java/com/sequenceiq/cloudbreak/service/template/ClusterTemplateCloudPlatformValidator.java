package com.sequenceiq.cloudbreak.service.template;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ClusterTemplateCloudPlatformValidator {

    private final Set<String> enabledPlatforms;

    public ClusterTemplateCloudPlatformValidator(@Value("${cb.enabledplatforms:}") Set<String> enabledPlatforms) {
        this.enabledPlatforms = enabledPlatforms;
    }

    public boolean isClusterTemplateCloudPlatformValid(String cloudPlatform) {
        return enabledPlatforms.contains(cloudPlatform) || CollectionUtils.isEmpty(enabledPlatforms);
    }

}
