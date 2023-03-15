package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class CloudbreakVersionCache extends AbstractCacheDefinition {

    public static final String CB_VERSION_CACHE = "cloudbreakVersionCache";

    private static final Long MAX_ENTRIES = 500L;

    private static final Long TTL_IN_MINUTES = 10L;

    @Override
    protected String getName() {
        return CB_VERSION_CACHE;
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
