package com.sequenceiq.cloudbreak.cloud.aws.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.CacheDefinition;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

import net.sf.ehcache.config.CacheConfiguration;

@Service
public class AwsCachingConfig implements CacheDefinition {

    public static final String TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential";
    private static final long TTL_IN_SECONDS = 5L * 60;
    private static final long MAX_ENTRIES = 1000L;

    @Override
    public CacheConfiguration cacheConfiguration() {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setMaxEntriesLocalHeap(MAX_ENTRIES);
        cacheConfiguration.setName(TEMPORARY_AWS_CREDENTIAL_CACHE);
        cacheConfiguration.setTimeToLiveSeconds(TTL_IN_SECONDS);
        return cacheConfiguration;
    }

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

    @Override
    public Class type() {
        return AwsCredentialView.class;
    }
}
