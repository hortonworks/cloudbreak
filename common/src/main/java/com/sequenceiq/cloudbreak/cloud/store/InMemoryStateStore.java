package com.sequenceiq.cloudbreak.cloud.store;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;

public final class InMemoryStateStore {
    private InMemoryStateStore() {

    }

    public static PollGroup getStack(Long key) {
        return InMemoryResourceStateStore.getResource("Stack", key);
    }

    public static Set<Long> getAllStackId() {
        return InMemoryResourceStateStore.getAllResourceId("Stack");
    }

    public static void putStack(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource("Stack", key, value);
    }

    public static void deleteStack(Long key) {
        InMemoryResourceStateStore.deleteResource("Stack", key);
    }

    public static PollGroup getCluster(Long key) {
        return InMemoryResourceStateStore.getResource("Cluster", key);
    }

    public static Iterable<Long> getAllClusterId() {
        return InMemoryResourceStateStore.getAllResourceId("Cluster");
    }

    public static void putCluster(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource("Cluster", key, value);
    }

    public static void deleteCluster(Long key) {
        InMemoryResourceStateStore.deleteResource("Cluster", key);
    }
}
