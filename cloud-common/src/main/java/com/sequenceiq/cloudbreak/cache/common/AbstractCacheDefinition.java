package com.sequenceiq.cloudbreak.cache.common;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.Cache;
import org.springframework.cache.guava.GuavaCache;

import com.google.common.cache.CacheBuilder;
import com.sequenceiq.cloudbreak.cache.CacheDefinition;

public abstract class AbstractCacheDefinition implements CacheDefinition {

    @Override
    public final Cache cacheConfiguration() {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .maximumSize(getMaxEntries())
                .expireAfterWrite(getTimeToLiveSeconds(), TimeUnit.SECONDS);
        return new GuavaCache(getName(), builder.build());
    }

    protected abstract String getName();

    protected abstract long getMaxEntries();

    protected abstract long getTimeToLiveSeconds();
}
