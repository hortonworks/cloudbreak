package com.sequenceiq.periscope.cache;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class ClusterProxyConfigurationCache extends AbstractCacheDefinition {

    public static final String CLUSTER_PROXY_CONFIGURATION_CACHE = "cpcCache";

    private static final long MAX_ENTRIES = 1L;

    private static final int TTL = 5 * 60;

    @Override
    protected String getName() {
        return CLUSTER_PROXY_CONFIGURATION_CACHE;
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
