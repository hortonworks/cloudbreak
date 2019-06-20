package com.sequenceiq.environment.store;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

public final class EnvironmentInMemoryStateStore {

    private static final String ENV_RESOUERCE_TYPE = "environment";

    private EnvironmentInMemoryStateStore() {
    }

    public static PollGroup get(Long key) {
        return InMemoryResourceStateStore.getResource(ENV_RESOUERCE_TYPE, key);
    }

    public static Set<Long> getAll() {
        return InMemoryResourceStateStore.getAllResourceId(ENV_RESOUERCE_TYPE);
    }

    public static void put(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource(ENV_RESOUERCE_TYPE, key, value);
    }

    public static void delete(Long key) {
        InMemoryResourceStateStore.deleteResource(ENV_RESOUERCE_TYPE, key);
    }
}
