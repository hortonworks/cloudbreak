package com.sequenceiq.cloudbreak.cloud.aws.common.cache;

import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

/**
 * Credential cache based on role or access key that can be used to skip unnecessary AWS verification calls
 */
@Service
public class AwsCredentialCachingConfig extends AbstractCacheDefinition {

    public static final String TEMPORARY_AWS_CREDENTIAL_VERIFIER_CACHE = "temporary_aws_credential_verifier";

    private static final long MAX_ENTRIES = 1000L;

    @Value("${cb.aws.credential.cache.ttl}")
    private long ttlInSeconds;

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
        return ttlInSeconds;
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
