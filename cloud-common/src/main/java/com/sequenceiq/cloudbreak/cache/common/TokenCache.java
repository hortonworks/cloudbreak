package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.stereotype.Service;

import net.sf.ehcache.config.SizeOfPolicyConfiguration;

@Service
public class TokenCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 15L * 60;

    private static final int MAX_DEPTH = 504;

    @Override
    protected String getName() {
        return "tokenCache";
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
        return TTL_IN_SECONDS;
    }

    @Override
    protected SizeOfPolicyConfiguration getSizeOfPolicyConfiguration() {
        return new SizeOfPolicyConfiguration().maxDepth(MAX_DEPTH);
    }
}
