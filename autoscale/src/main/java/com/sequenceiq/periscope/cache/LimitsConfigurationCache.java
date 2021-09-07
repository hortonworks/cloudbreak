package com.sequenceiq.periscope.cache;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class LimitsConfigurationCache extends AbstractCacheDefinition {

    public static final String LIMITS_CONFIGURATION_CACHE = "limitsConfigurationCache";

    private static final long MAX_ENTRIES = 1L;

    private static final int TTL = 10 * 60;

    @Override
    protected String getName() {
        return LIMITS_CONFIGURATION_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL;
    }
}
