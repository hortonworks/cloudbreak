package com.sequenceiq.cloudbreak.auth.altus.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class UmsAuthorizationEntitlementRegisteredCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${altus.ums.account.cache.ttl:5}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "umsAuthorizationEntitlementRegisteredCache";
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