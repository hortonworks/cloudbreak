package com.sequenceiq.cloudbreak.service.stack;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Service
public class TargetedUpscaleCache extends AbstractCacheDefinition {

    private static final Long MAX_ENTRIES = 1000L;

    @Value("${cb.upscale.targeted.cache.ttl}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "targetedUpscaleCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return ttlMinutes == 0L ? TimeUnit.MINUTES.toSeconds(2) : TimeUnit.MINUTES.toSeconds(ttlMinutes);
    }
}
