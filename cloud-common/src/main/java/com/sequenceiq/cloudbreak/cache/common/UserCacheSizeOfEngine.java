package com.sequenceiq.cloudbreak.cache.common;

import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

public class UserCacheSizeOfEngine extends DefaultSizeOfEngine {

    private static final int MAX_DEPTH = 2;

    private static final boolean ABORT_WHEN_MAX_DEPTH_EXCEEDED = true;

    public UserCacheSizeOfEngine() {
        super(MAX_DEPTH, ABORT_WHEN_MAX_DEPTH_EXCEEDED);
    }
}
