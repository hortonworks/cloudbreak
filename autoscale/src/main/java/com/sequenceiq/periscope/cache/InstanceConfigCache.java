package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class InstanceConfigCache extends AbstractCacheDefinition {

    public static final String INSTANCE_CONFIG_CACHE = "instanceConfigCache";

    private static final long MAX_ENTRIES = 5000L;

    private static final int TTL_IN_MINUTES = 30;

    @Override
    protected String getName() {
        return INSTANCE_CONFIG_CACHE;
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
