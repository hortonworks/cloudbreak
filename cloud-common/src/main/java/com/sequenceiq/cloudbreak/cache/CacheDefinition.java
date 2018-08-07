package com.sequenceiq.cloudbreak.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKeyGenerator;

import net.sf.ehcache.config.CacheConfiguration;

public interface CacheDefinition {

    CacheConfiguration cacheConfiguration();

    default Object generateKey(Object target, Method method, Object... params) {
        return SimpleKeyGenerator.generateKey(params);
    }

    default Class<?> type() {
        return null;
    }
}
