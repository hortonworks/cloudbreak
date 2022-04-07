package com.sequenceiq.cloudbreak.service.stackpatch.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@Configuration
@ConfigurationProperties("existing-stack-patcher.patch-configs.cluster-public-endpoint")
public class ClusterPublicEndpointPatchConfig extends StackPatchTypeConfig {

    private Set<Long> relatedStacks = new HashSet<>();

    public Set<Long> getRelatedStacks() {
        return relatedStacks;
    }

    public void setRelatedStacks(Set<Long> relatedStacks) {
        this.relatedStacks = relatedStacks;
    }

}
