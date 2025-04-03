package com.sequenceiq.flow.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;

public class TestStateStore {

    private static final Map<Long, PollGroup> POLL_GROUP_MAP = new ConcurrentHashMap<>();

    private TestStateStore() {
    }

    public static void put(long id, PollGroup pollGroup) {
        POLL_GROUP_MAP.put(id, pollGroup);
    }

    public static PollGroup get(long id) {
        return POLL_GROUP_MAP.get(id);
    }

    public static void delete(long id) {
        POLL_GROUP_MAP.remove(id);
    }
}
