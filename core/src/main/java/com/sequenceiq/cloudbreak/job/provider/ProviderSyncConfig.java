package com.sequenceiq.cloudbreak.job.provider;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.ResourceType;

@Component
public class ProviderSyncConfig {

    @Value("${provider-sync.intervalminutes:360}")
    private int intervalInMinutes;

    @Value("${provider-sync.enabled:false}")
    private boolean enabled;

    @Value("#{'${provider-sync.enabled-providers}'.split(',')}")
    private Set<String> enabledProviders;

    @Value("#{'${provider-sync.resourcetype-list}'.split(',')}")
    private Set<ResourceType> resourceTypeList;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isProviderSyncEnabled() {
        return enabled;
    }

    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }

    public Set<ResourceType> getResourceTypeList() {
        return resourceTypeList;
    }
}