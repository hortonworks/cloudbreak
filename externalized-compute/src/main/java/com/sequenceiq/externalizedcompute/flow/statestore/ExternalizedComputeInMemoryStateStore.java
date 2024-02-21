package com.sequenceiq.externalizedcompute.flow.statestore;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

public final class ExternalizedComputeInMemoryStateStore {

    private static final String RESOURCE_TYPE = "EXTERNALIZED_COMPUTE";

    private ExternalizedComputeInMemoryStateStore() {
    }

    public static PollGroup get(Long key) {
        return InMemoryResourceStateStore.getResource(RESOURCE_TYPE, key);
    }

    public static Set<Long> getAll() {
        return InMemoryResourceStateStore.getAllResourceId(RESOURCE_TYPE);
    }

    public static void put(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource(RESOURCE_TYPE, key, value);
    }

    public static void delete(Long key) {
        InMemoryResourceStateStore.deleteResource(RESOURCE_TYPE, key);
    }

}
