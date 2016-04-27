package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ConfigureSssdRequest extends AbstractClusterUpscaleRequest {

    public ConfigureSssdRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
