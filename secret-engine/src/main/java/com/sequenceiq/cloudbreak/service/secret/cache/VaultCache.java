package com.sequenceiq.cloudbreak.service.secret.cache;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;
import com.sequenceiq.cloudbreak.vault.VaultConstants;

@Service
public class VaultCache extends AbstractCacheDefinition {

    // Average key is  300 chars, average value is less than 4500 chars plus adding some headroom
    // roughly it is 10kb per entry due to UTF-16
    // blueprints, templates are huge sometimes around 30kb, but majority of the secrets are less than 50 chars
    // So the estimate is 10000 * 10kb = 100MB
    private static final long MAX_ENTRIES = 10000L;

    // Maximum 60sec is allowed for the secret to be in the cache, because the secret rotation is not updating the path, and other pod requires 60sect to get
    // to take over the flow
    private static final long TTL_IN_SECONDS = 60L;

    @Override
    protected String getName() {
        return VaultConstants.CACHE_NAME;
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
