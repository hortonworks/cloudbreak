package com.sequenceiq.cloudbreak.cache.common;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudResourceAzCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 10_000L;

    @Value("${cb.cloud.region.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "cloudResourceAzCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return ttlMinutes == 0L ? 1 : TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }
}
