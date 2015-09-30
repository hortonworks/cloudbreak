package com.sequenceiq.cloudbreak.conf;

import java.lang.reflect.Method;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

import net.sf.ehcache.config.CacheConfiguration;

@Configuration
@EnableCaching
@EnableAutoConfiguration
public class CachingConfig implements CachingConfigurer {

    public static final String TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential";
    private static final long TTL_IN_SECONDS = 5L * 60;
    private static final long MAX_ENTRIES = 1000L;

    @Bean
    public net.sf.ehcache.CacheManager ehCacheManager() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("userCache");
        cacheConfiguration.setMemoryStoreEvictionPolicy("LRU");
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);

        CacheConfiguration awsCacheConfiguration = new CacheConfiguration();
        awsCacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        awsCacheConfiguration.setName(TEMPORARY_AWS_CREDENTIAL_CACHE);
        awsCacheConfiguration.setTimeToLiveSeconds(TTL_IN_SECONDS);

        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(cacheConfiguration);
        config.addCache(awsCacheConfiguration);

        return net.sf.ehcache.CacheManager.newInstance(config);
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheManager());
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new SpecificKeyGenerator();
    }

    private static class SpecificKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            if (params.length == 0) {
                return SimpleKey.EMPTY;
            }
            if (params.length == 1) {
                if (params[0] instanceof AwsCredentialView) {
                    AwsCredentialView param = (AwsCredentialView) params[0];
                    if (param.getId() != null) {
                        return param.getId();
                    } else {
                        return SimpleKey.EMPTY;
                    }
                } else {
                    Object param = params[0];
                    if (param != null && !param.getClass().isArray()) {
                        return param;
                    }
                }
            }
            return new SimpleKey(params);
        }
    }

}