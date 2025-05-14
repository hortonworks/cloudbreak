package com.sequenceiq.cloudbreak.service.upgrade.validation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class CmUrlCache extends AbstractCacheDefinition {

    public static final String CM_URL_CACHE = "cmUrlCache";

    private static final long MAX_ENTRIES = 1000L;

    private static final long TTL = 15L * 60L;

    @Override
    protected String getName() {
        return CM_URL_CACHE;
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
