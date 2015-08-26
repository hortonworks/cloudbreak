package com.sequenceiq.cloudbreak.cloud.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequenceiq.cloudbreak.domain.Status;

public final class InMemoryStateStore {

    private static final Map<Long, Status> STATE_STORE = new ConcurrentHashMap<>();

    private InMemoryStateStore() {

    }

    public static Status get(Long key) {
        return STATE_STORE.get(key);
    }

    public static void put(Long key, Status value) {
        STATE_STORE.put(key, value);
    }

    public static void delete(Long key) {
        STATE_STORE.remove(key);
    }
}
