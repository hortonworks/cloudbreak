package com.sequenceiq.cloudbreak.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.sf.ehcache.config.CacheConfiguration;

@Service
public class ResourceDefinitionCache implements CacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${cb.cloud.definition.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("resourceDefinitionCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        cacheConfiguration.setTimeToLiveSeconds(ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes));
        return cacheConfiguration;
    }

}
