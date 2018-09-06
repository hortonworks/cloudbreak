package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.stereotype.Service;

@Service
public class IdentityUserCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 15L * 60;

    @Override
    protected String getName() {
        return "identityUserCache";
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

}
