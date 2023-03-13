package com.sequenceiq.cloudbreak.cloud.aws.common.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

@Service
public class AwsStsAssumeRoleCredentialsProviderCacheConfig extends AbstractCacheDefinition {

    public static final String TEMPORARY_AWS_STS_ASSUMEROLE_CREDENTIALS_PROVIDER_CACHE = "temporary_aws_sts_assumerole_credentials_provider";

    private static final long TTL_IN_SECONDS = 5L * 60;

    private static final long MAX_ENTRIES = 1000L;

    @Override
    public Object generateKey(Object target, Method method, Object... params) {
        if (params.length == 1) {
            AwsCredentialView param = (AwsCredentialView) params[0];
            return param.getCredentialCrn() != null ? param.getCredentialCrn() : SimpleKey.EMPTY;
        }
        return SimpleKey.EMPTY;
    }

    @Override
    public Class<?> type() {
        return AwsCredentialView.class;
    }

    @Override
    protected String getName() {
        return TEMPORARY_AWS_STS_ASSUMEROLE_CREDENTIALS_PROVIDER_CACHE;
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
