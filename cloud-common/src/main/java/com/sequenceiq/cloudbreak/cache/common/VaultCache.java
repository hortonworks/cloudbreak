package com.sequenceiq.cloudbreak.cache.common;

import org.springframework.stereotype.Service;

@Service
public class VaultCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL_IN_SECONDS = 5L;

    @Override
    protected String getName() {
        return "vaultCache";
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
