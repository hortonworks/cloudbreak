package com.sequenceiq.periscope.service.security;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class StackAccessByOwnerCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 5L * 60;

    @Override
    protected String getName() {
        return "stackAccessByOwnerCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL_IN_SECONDS;
    }
}
