package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class DependentHostGroupsCache extends AbstractCacheDefinition {

    public static final String DEPENDENT_HOST_GROUPS_CACHE = "dependentHostGroupsCache";

    private static final Long MAX_ENTRIES = 500L;

    private static final Integer TTL_MINUTES = 10;

    @Override
    protected String getName() {
        return DEPENDENT_HOST_GROUPS_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TimeUnit.MINUTES.toSeconds(TTL_MINUTES);
    }
}
