package com.sequenceiq.cloudbreak.cloud.aws.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@Service
public class AwsCachingConfig extends AbstractCacheDefinition {

    public static final String TEMPORARY_AWS_CREDENTIAL_CACHE = "temporary_aws_credential";

    private static final long TTL_IN_SECONDS = 5L * 60;

    private static final long MAX_ENTRIES = 1000L;

    @Override
    public Object generateKey(Object target, Method method, Object... params) {
        if (params.length == 1) {
            AwsCredentialView param = (AwsCredentialView) params[0];
            return param.getId() != null ? param.getId() : SimpleKey.EMPTY;
        }
        return SimpleKey.EMPTY;
    }

    @Override
    public Class<?> type() {
        return AwsCredentialView.class;
    }

    @Override
    protected String getName() {
        return TEMPORARY_AWS_CREDENTIAL_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL_IN_SECONDS;
    }
}
