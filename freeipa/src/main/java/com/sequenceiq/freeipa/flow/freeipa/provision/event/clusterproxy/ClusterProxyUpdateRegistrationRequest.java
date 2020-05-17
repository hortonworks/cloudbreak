package com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy;

import java.util.List;

import com.sequenceiq.freeipa.flow.instance.InstanceEvent;

public class ClusterProxyUpdateRegistrationRequest extends InstanceEvent {
    public ClusterProxyUpdateRegistrationRequest(Long stackId) {
        super(stackId);
    }

    public ClusterProxyUpdateRegistrationRequest(Long stackId, List<String> instanceIds) {
        super(stackId, instanceIds);
    }
}
