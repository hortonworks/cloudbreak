package com.sequenceiq.environment.store;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

public final class EnvironmentInMemoryStateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentInMemoryStateStore.class);

    private static final String ENV_RESOUERCE_TYPE = "environment";

    private EnvironmentInMemoryStateStore() {
    }

    public static PollGroup get(Long key) {
        LOGGER.debug("Getting pollgroup from in memory store with key {}.", key);
        return InMemoryResourceStateStore.getResource(ENV_RESOUERCE_TYPE, key);
    }

    public static Set<Long> getAll() {
        LOGGER.debug("Getting all pollgroup from in memory store.");
        return InMemoryResourceStateStore.getAllResourceId(ENV_RESOUERCE_TYPE);
    }

    public static void put(Long key, PollGroup value) {
        LOGGER.debug("Updating pollgroup from in memory store with key {} and value {}.", key, value);
        InMemoryResourceStateStore.putResource(ENV_RESOUERCE_TYPE, key, value);
    }

    public static void delete(Long key) {
        LOGGER.debug("Deleting pollgroup from in memory store with key {}.", key);
        InMemoryResourceStateStore.deleteResource(ENV_RESOUERCE_TYPE, key);
    }
}
