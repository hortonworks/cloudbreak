package com.sequenceiq.cloudbreak.auth.altus.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class UmsMachineUserCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${altus.ums.machineuser.cache.ttl:1}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "umsMachineUserCache";
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
