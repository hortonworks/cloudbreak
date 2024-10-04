package com.sequenceiq.cloudbreak.database;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class RdsIamAuthenticationTokenCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 500L;

    @Value("${rds.iam.auth.token.cache.ttl:1}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "rdsIamAuthenticationTokenCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }
}
