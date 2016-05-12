package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ConfigureSssdResult extends AbstractClusterScaleResult<ConfigureSssdRequest> {

    public ConfigureSssdResult(ConfigureSssdRequest request) {
        super(request);
    }

    public ConfigureSssdResult(String statusReason, Exception errorDetails, ConfigureSssdRequest request) {
        super(statusReason, errorDetails, request);
    }
}
