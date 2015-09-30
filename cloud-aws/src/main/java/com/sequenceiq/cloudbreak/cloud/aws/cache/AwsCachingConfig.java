package com.sequenceiq.cloudbreak.cloud.aws.cache;

import java.lang.reflect.Method;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

import net.sf.ehcache.config.CacheConfiguration;

//@Configuration
//@EnableCaching
//@EnableAutoConfiguration
public class AwsCachingConfig implements CachingConfigurer {

    public static final String TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential";
    private static final long TTL_IN_SECONDS = 5L * 60;
    private static final long MAX_ENTRIES = 1000L;

    @Bean
    public net.sf.ehcache.CacheManager awsehCacheManager() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        cacheConfiguration.setName(TEMPORARY_AWS_CREDENTIAL_CACHE);
        cacheConfiguration.setTimeToLiveSeconds(TTL_IN_SECONDS);

        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(cacheConfiguration);

        return net.sf.ehcache.CacheManager.newInstance(config);
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(awsehCacheManager());
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new AwsCredentialViewKeyGenerator();
    }

    private static class AwsCredentialViewKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            if (params.length == 0) {
                return SimpleKey.EMPTY;
            }
            if (params.length == 1) {
                AwsCredentialView param = (AwsCredentialView) params[0];
                if (param.getId() != null) {
                    return param.getId();
                } else {
                    return SimpleKey.EMPTY;
                }
            }
            return SimpleKey.EMPTY;
        }
    }
}
