package com.sequenceiq.cloudbreak.cache.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;

import net.sf.ehcache.config.CacheConfiguration;

@Service
public class ImageCatalogCache implements CacheDefinition {
    private static final long MAX_ENTRIES = 1000L;

    @Value("${cb.image.catalog.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("imageCatalogCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        //Cache cannot be turned off, the default value is 0, which means no timeToLive (TTL) eviction takes place (infinite lifetime).
        cacheConfiguration.setTimeToLiveSeconds(ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes));
        return cacheConfiguration;
    }
}
