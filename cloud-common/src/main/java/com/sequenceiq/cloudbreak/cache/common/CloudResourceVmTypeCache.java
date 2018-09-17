package com.sequenceiq.cloudbreak.cache.common;


import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudResourceVmTypeCache extends AbstractCacheDefinition {

    private static final long MAX_ENTRIES = 1000L;

    @Value("${cb.cloud.vmtype.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return "cloudResourceVmTypeCache";
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
