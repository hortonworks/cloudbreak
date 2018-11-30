package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public abstract class AmbariComponentsRequest extends AbstractClusterScaleRequest {

    private final String hostName;

    private final Map<String, String> components;

    protected AmbariComponentsRequest(Long stackId, String hostGroupName, String hostName, Map<String, String> components) {
        super(stackId, hostGroupName);
        this.components = components;
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }

    public Map<String, String> getComponents() {
        return components;
    }
}
