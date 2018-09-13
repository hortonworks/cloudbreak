package com.sequenceiq.cloudbreak.cache.common;


import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.sf.ehcache.config.SizeOfPolicyConfiguration;

@Service
public class CloudResourceRegionCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final int MAX_DEPTH = 500;

    @Value("${cb.cloud.region.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "cloudResourceRegionCache";
    }

    @Override
    protected String getMemoryStoreEvictionPolicy() {
        return "LRU";
    }

    @Override
    protected long getMaxEntriesLocalHeap() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getMaxBytesLocalHeap() {
        return 0L;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }

    @Override
    protected SizeOfPolicyConfiguration getSizeOfPolicyConfiguration() {
        return new SizeOfPolicyConfiguration().maxDepth(MAX_DEPTH);
    }
}
