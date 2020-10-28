package com.sequenceiq.cloudbreak.auth.altus.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class UmsUserHasRightsForResourceCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    /**
     * @deprecated {@link #ttlMinutes} was replaced by {@link #ttlSeconds} because it was not providing enough flexibility on ttl.
     */
    @Value("${altus.ums.rights.cache.ttl:1}")
    @Deprecated
    private long ttlMinutes;

    @Value("${altus.ums.rights.cache.seconds.ttl:0}")
    private long ttlSeconds;

    @Override
    protected String getName() {
        return "umsUserHasRightsForResourceCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        if (ttlSeconds != 0) {
            return ttlSeconds;
        }
        return ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }
}
