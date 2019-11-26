package com.sequenceiq.cloudbreak.cloud.aws.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

/**
 * Credential cache based on role or access key that can be used to skip unnecessary AWS verification calls
 */
@Service
public class AwsCredentialCachingConfig extends AbstractCacheDefinition {

    public static final String TEMPORARY_AWS_CREDENTIAL_VERIFIER_CACHE = "temporary_aws_credential_verifier";

    private static final long TTL_IN_SECONDS = 5L;

    private static final long MAX_ENTRIES = 1000L;

    @Override
    public String getName() {
        return TEMPORARY_AWS_CREDENTIAL_VERIFIER_CACHE;
    }

    @Override
    public long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    public long getTimeToLiveSeconds() {
        return TTL_IN_SECONDS;
    }

    @Override
    public Class<?> type() {
        return AwsCredentialView.class;
    }

    @Override
    public Object generateKey(Object target, Method method, Object... params) {
        if (params.length == 1) {
            AwsCredentialView param = (AwsCredentialView) params[0];
            if (param.getRoleArn() != null) {
                return param.getRoleArn();
            } else if (param.getAccessKey() != null) {
                return param.getAccessKey();
            }
        }
        return SimpleKey.EMPTY;
    }
}
