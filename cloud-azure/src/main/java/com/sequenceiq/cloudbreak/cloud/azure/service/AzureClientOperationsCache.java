package com.sequenceiq.cloudbreak.cloud.azure.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class AzureClientOperationsCache extends AbstractCacheDefinition {

    public static final String AZURE_CLIENT_OPERATIONS_CACHE = "azureClientOperationsCache";

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL = 60;

    @Override
    protected String getName() {
        return AZURE_CLIENT_OPERATIONS_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL;
    }
}
