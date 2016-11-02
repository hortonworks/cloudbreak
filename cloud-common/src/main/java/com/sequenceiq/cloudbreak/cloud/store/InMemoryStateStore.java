package com.sequenceiq.cloudbreak.cloud.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;

public final class InMemoryStateStore {

    private static final Map<Long, PollGroup> STACK_STATE_STORE = new ConcurrentHashMap<>();

    private static final Map<Long, PollGroup> CLUSTER_STATE_STORE = new ConcurrentHashMap<>();

    private InMemoryStateStore() {

    }

    public static PollGroup getStack(Long key) {
        return STACK_STATE_STORE.get(key);
    }

    public static void putStack(Long key, PollGroup value) {
        STACK_STATE_STORE.put(key, value);
    }

    public static void deleteStack(Long key) {
        STACK_STATE_STORE.remove(key);
    }

    public static PollGroup getCluster(Long key) {
        return CLUSTER_STATE_STORE.get(key);
    }

    public static void putCluster(Long key, PollGroup value) {
        CLUSTER_STATE_STORE.put(key, value);
    }

    public static void deleteCluster(Long key) {
        CLUSTER_STATE_STORE.remove(key);
    }
}
