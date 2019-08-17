package com.sequenceiq.cloudbreak.auth.altus.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class UmsRolesCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${altus.ums.roles.cache.ttl:1}")
    private long ttlHours;

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
        return ttlHours == 0L ? 1 : TimeUnit.HOURS.toSeconds(ttlHours);
    }
}
