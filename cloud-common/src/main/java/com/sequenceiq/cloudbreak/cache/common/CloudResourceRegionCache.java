package com.sequenceiq.cloudbreak.cache.common;


import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;

import net.sf.ehcache.config.CacheConfiguration;

@Service
public class CloudResourceRegionCache implements CacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("cloudResourceRegionCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        return cacheConfiguration;
    }
}
