package com.sequenceiq.cloudbreak.cloud.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;

public final class InMemoryStateStore {

    private static final Map<Long, PollGroup> STATE_STORE = new ConcurrentHashMap<>();

    private InMemoryStateStore() {

    }

    public static PollGroup get(Long key) {
        return STATE_STORE.get(key);
    }

    public static void put(Long key, PollGroup value) {
        STATE_STORE.put(key, value);
    }

    public static void delete(Long key) {
        STATE_STORE.remove(key);
    }
}
