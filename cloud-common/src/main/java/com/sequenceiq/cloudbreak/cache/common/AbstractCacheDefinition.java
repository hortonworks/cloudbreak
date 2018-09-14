package com.sequenceiq.cloudbreak.cache.common;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;

public abstract class AbstractCacheDefinition implements CacheDefinition {

    @Override
    public final CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName(getName());
        cacheConfiguration.setMemoryStoreEvictionPolicy(getMemoryStoreEvictionPolicy());
        cacheConfiguration.setMaxEntriesLocalHeap(getMaxEntriesLocalHeap());
        cacheConfiguration.setTimeToLiveSeconds(getTimeToLiveSeconds());
        cacheConfiguration.addSizeOfPolicy(getSizeOfPolicyConfiguration());
        return cacheConfiguration;
    }

    protected abstract String getName();

    protected abstract String getMemoryStoreEvictionPolicy();

    protected abstract long getMaxEntriesLocalHeap();

    protected abstract long getMaxBytesLocalHeap();

    protected abstract long getTimeToLiveSeconds();

    protected abstract SizeOfPolicyConfiguration getSizeOfPolicyConfiguration();
}
