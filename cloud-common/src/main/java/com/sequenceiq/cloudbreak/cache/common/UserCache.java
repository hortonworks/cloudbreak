package com.sequenceiq.cloudbreak.cache.common;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

@Service
public class UserCache extends AbstractCacheDefinition {

    private static final long MAX_BYTES =  10 * 1024L;

    private static final long TTL_IN_SECONDS = 5L * 60;

    @PostConstruct
    public void init() {
        System.setProperty("net.sf.ehcache.sizeofengine.default.userCache", UserCacheSizeOfEngine.class.getCanonicalName());
    }

    @Override
    protected String getName() {
        return "userCache";
    }

    @Override
    protected String getMemoryStoreEvictionPolicy() {
        return "LRU";
    }

    @Override
    protected long getMaxEntriesLocalHeap() {
        return 0L;
    }

    @Override
    protected long getMaxBytesLocalHeap() {
        return MAX_BYTES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TTL_IN_SECONDS;
    }
}
