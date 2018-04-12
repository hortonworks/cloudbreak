package com.sequenceiq.cloudbreak.cache;

import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.stereotype.Service;

@Service
public class ResourceDefinitionCache implements CacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("resourceDefinitionCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        return cacheConfiguration;
    }

}
