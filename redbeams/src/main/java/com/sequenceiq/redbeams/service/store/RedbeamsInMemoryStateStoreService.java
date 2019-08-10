package com.sequenceiq.redbeams.service.store;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;

/**
 * Needed for HA: a given node will store flows in this map it started. Heartbeatservice will check regularly if another node
 * has not initiated termination and thus needs to know about all running flows.
 */
@Service
public class RedbeamsInMemoryStateStoreService {
    private static final String REDBEAMS_RESOUERCE_TYPE = "redbeams";

    public Set<Long> getAll() {
        return InMemoryResourceStateStore.getAllResourceId(REDBEAMS_RESOUERCE_TYPE);
    }

    public void registerStart(Long key) {
        put(key, PollGroup.POLLABLE);
    }

    public void registerCancel(Long key) {
        put(key, PollGroup.CANCELLED);
    }

    public void delete(Long key) {
        InMemoryResourceStateStore.deleteResource(REDBEAMS_RESOUERCE_TYPE, key);
    }

    private void put(Long key, PollGroup value) {
        InMemoryResourceStateStore.putResource(REDBEAMS_RESOUERCE_TYPE, key, value);
    }

}
