package com.sequenceiq.cloudbreak.cache.common;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sequenceiq.cloudbreak.cache.CacheDefinition;

public abstract class AbstractCacheDefinition implements CacheDefinition {

    @Override
    public final Cache cacheConfiguration() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(getMaxEntries())
                .expireAfterWrite(getTimeToLiveSeconds(), TimeUnit.SECONDS);
        return new CaffeineCache(getName(), builder.build());
    }

    protected abstract String getName();

    protected abstract long getMaxEntries();

    protected abstract long getTimeToLiveSeconds();
}
