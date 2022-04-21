package com.sequenceiq.cloudbreak.auth.altus.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class UmsRolesCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${altus.ums.roles.cache.seconds.ttl:300}")
    private long ttlSeconds;

    @Override
    protected String getName() {
        return "umsRolesCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return ttlSeconds;
    }
}
