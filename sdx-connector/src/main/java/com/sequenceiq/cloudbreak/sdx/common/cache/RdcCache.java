package com.sequenceiq.cloudbreak.sdx.common.cache;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class RdcCache extends AbstractCacheDefinition {

    public static final String RDC_CACHE = "rdcCache";

    private static final long MAX_ENTRIES = 100L;

    private static final long TTL_SECONDS = 60L * 60L;

    @Override
    protected String getName() {
        return RDC_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL_SECONDS;
    }
}
