package com.sequenceiq.cloudbreak.monitoring;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cache.common.AbstractNoTtlCacheDefinition;

@Service
public class MonitoringEnablementCache extends AbstractNoTtlCacheDefinition {

    private static final long MAX_ENTRIES = 5000L;

    @Override
    protected String getName() {
        return "monitoringEnablementCache";
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }
}
