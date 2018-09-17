package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.stereotype.Service;

@Service
public class IdentityUserCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 15L * 60;

    @Override
    protected String getName() {
        return "identityUserCache";
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
