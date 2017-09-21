package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudGateWays {

    private Map<String, Set<CloudGateWay>> cloudGateWayResponses = new HashMap<>();

    public CloudGateWays() {
    }

    public CloudGateWays(Map<String, Set<CloudGateWay>> cloudGateWayResponses) {
        this.cloudGateWayResponses = cloudGateWayResponses;
    }

    public Map<String, Set<CloudGateWay>> getCloudGateWayResponses() {
        return cloudGateWayResponses;
    }
}
