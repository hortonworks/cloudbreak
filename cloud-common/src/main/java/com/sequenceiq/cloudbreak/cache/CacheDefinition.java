package com.sequenceiq.cloudbreak.cache;

import java.lang.reflect.Method;

import net.sf.ehcache.config.CacheConfiguration;

public interface CacheDefinition {

    CacheConfiguration cacheConfiguration();

    Object generate(Object target, Method method, Object... params);

    Class type();
}
