package com.sequenceiq.cloudbreak.cache;

import java.lang.reflect.Method;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

public interface CacheDefinition {

    Cache cacheConfiguration();

    default Object generateKey(Object target, Method method, Object... params) {
        return SimpleKeyGenerator.generateKey(params);
    }

    default Class<?> type() {
        return null;
    }
}
