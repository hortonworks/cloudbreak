package com.sequenceiq.cloudbreak.service.sslcontext;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sequenceiq.cloudbreak.cache.CacheDefinition;

@Service
public class SSLContextCache implements CacheDefinition {

    private static final long SSL_CONTEXT_MAX_ENTRIES = 1000L;

    private static final String SSL_CONTEXT_CACHE_NAME = "sslContextCache";

    @Value("${sslcontext.cache.ttl:1440}")
    private long ttlMinutes;

    @Override
    public Cache cacheConfiguration() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .softValues()
                .maximumSize(SSL_CONTEXT_MAX_ENTRIES)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES);
        return new CaffeineCache(SSL_CONTEXT_CACHE_NAME, builder.build());
    }
}
