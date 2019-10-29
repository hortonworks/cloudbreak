package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class SecurityConfigCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 10000L;

    private static final long TTL_IN_MINUTES = 5L;

    @Override
    protected String getName() {
        return "securityConfigCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TimeUnit.MINUTES.toSeconds(TTL_IN_MINUTES);
    }
}
