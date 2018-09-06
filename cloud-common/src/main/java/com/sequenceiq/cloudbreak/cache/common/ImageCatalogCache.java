package com.sequenceiq.cloudbreak.cache.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageCatalogCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${cb.image.catalog.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "imageCatalogCache";
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
}
