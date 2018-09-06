package com.sequenceiq.cloudbreak.cache.common;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;

import net.sf.ehcache.config.CacheConfiguration;

public abstract class AbstractCacheDefinition implements CacheDefinition {

    @Override
    public final CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName(getName());
        cacheConfiguration.setMemoryStoreEvictionPolicy(getMemoryStoreEvictionPolicy());
        if (getMaxEntriesLocalHeap() > 0) {
            cacheConfiguration.setMaxEntriesLocalHeap(getMaxEntriesLocalHeap());
        }
        if (getMaxBytesLocalHeap() > 0) {
            cacheConfiguration.setMaxBytesLocalHeap(getMaxBytesLocalHeap());
        }
        cacheConfiguration.setTimeToLiveSeconds(getTimeToLiveSeconds());
        return cacheConfiguration;
    }

    protected abstract String getName();

    protected abstract String getMemoryStoreEvictionPolicy();

    protected abstract long getMaxEntriesLocalHeap();

    protected abstract long getMaxBytesLocalHeap();

    protected abstract long getTimeToLiveSeconds();
}
