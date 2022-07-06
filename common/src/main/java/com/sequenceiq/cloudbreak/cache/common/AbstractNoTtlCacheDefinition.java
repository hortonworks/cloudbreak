package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sequenceiq.cloudbreak.cache.CacheDefinition;

public abstract class AbstractNoTtlCacheDefinition implements CacheDefinition {

    @Override
    public final Cache cacheConfiguration() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .maximumSize(getMaxEntries());
        return new CaffeineCache(getName(), builder.build());
    }

    protected abstract String getName();

    protected abstract long getMaxEntries();
}
