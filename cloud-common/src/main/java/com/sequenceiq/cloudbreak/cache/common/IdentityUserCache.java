package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;

import net.sf.ehcache.config.CacheConfiguration;

@Service
public class IdentityUserCache implements CacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 15L * 60;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("identityUserCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        cacheConfiguration.setTimeToLiveSeconds(TTL_IN_SECONDS);
        return cacheConfiguration;
    }

}
