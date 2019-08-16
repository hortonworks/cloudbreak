package com.sequenceiq.datalake.flow.statestore;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

public final class DatalakeInMemoryStateStore {

    private static final String SDX_RESOURCE_TYPE = "SDX";

    private DatalakeInMemoryStateStore() {
    }

    public static PollGroup get(Long key) {
        return InMemoryResourceStateStore.getResource(SDX_RESOURCE_TYPE, key);
    }

    public static Set<Long> getAll() {
        return InMemoryResourceStateStore.getAllResourceId(SDX_RESOURCE_TYPE);
    }

    public static void put(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource(SDX_RESOURCE_TYPE, key, value);
    }

    public static void delete(Long key) {
        InMemoryResourceStateStore.deleteResource(SDX_RESOURCE_TYPE, key);
    }

}
