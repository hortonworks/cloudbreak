package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class DependentHostGroupsCache extends AbstractCacheDefinition {

    public static final String DEPENDENT_HOST_GROUPS_CACHE = "dependentHostGroupsCache";

    @Value("${cb.dependentHostGroups.max.entries:500}")
    private long maxEntries;

    @Value("${cb.dependentHostGroups.cache.ttl:10}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return DEPENDENT_HOST_GROUPS_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return maxEntries;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }
}
