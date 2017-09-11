package com.sequenceiq.cloudbreak.cache;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableAutoConfiguration
public class CachingConfig implements CachingConfigurer {

    @Inject
    private List<CacheDefinition> cacheDefinitions;

    private final Map<Class, CacheDefinition> classCacheDefinitionMap = new HashMap<>();

    @PostConstruct
    public void postCachDefinition() {
        for (CacheDefinition cacheDefinition : cacheDefinitions) {
            if (cacheDefinition.type() != null) {
                classCacheDefinitionMap.put(cacheDefinition.type(), cacheDefinition);
            }
        }
    }

    @Bean
    public net.sf.ehcache.CacheManager ehCacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        for (CacheDefinition cacheDefinition : cacheDefinitions) {
            config.addCache(cacheDefinition.cacheConfiguration());
        }
        return net.sf.ehcache.CacheManager.newInstance(config);
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheManager());
    }

    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SpecificKeyGenerator();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }

    private class SpecificKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            if (params.length == 1) {
                CacheDefinition cacheDefinition = classCacheDefinitionMap.get(params[0].getClass());
                if (cacheDefinition != null) {
                    return cacheDefinition.generateKey(target, method, params);
                }
            }
            return SimpleKeyGenerator.generateKey(params);
        }
    }

}