package com.sequenceiq.freeipa.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class EncryptionProfileCache extends AbstractCacheDefinition {
    public static final String FREEIPA_ENCRYPTION_PROFILE_CACHE = "freeipaEncryptionProfileCache";

    private static final long MAX_ENTRIES = 20L;

    @Value("${freeipa.env.cache.ttl:15}")
    private long ttlMinutes;

    @Override
    protected String getName() {
        return FREEIPA_ENCRYPTION_PROFILE_CACHE;
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
