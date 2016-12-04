package com.sequenceiq.cloudbreak.shell.util;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.TopologyResponse;

public class TopologyUtil {
    private TopologyUtil() {
    }

    public static void checkTopologyForResource(Set<TopologyResponse> publics, Long topologyId, String platform) {
        if (publics != null && topologyId != null) {
            for (TopologyResponse t : publics) {
                if (t.getId().equals(topologyId)) {
                    if (t.getCloudPlatform().equals(platform)) {
                        return;
                    } else {
                        throw new RuntimeException("The selected platform belongs to a different cloudplatform.");
                    }
                }
            }
            throw new RuntimeException("Not found platform with id: " + topologyId);
        }
    }
}
