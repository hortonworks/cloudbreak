package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class AutoscaleRecommendationCache extends AbstractCacheDefinition {

    public static final String CACHE_NAME = "autoscaleRecommendationCache";

    private static final long MAX_ENTRIES = 100L;

    private static final int TTL_IN_MINUTES = 5;

    @Override
    protected String getName() {
        return CACHE_NAME;
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
