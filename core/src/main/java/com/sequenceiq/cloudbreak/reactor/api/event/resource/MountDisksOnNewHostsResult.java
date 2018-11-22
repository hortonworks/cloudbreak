package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class MountDisksOnNewHostsResult extends AbstractClusterBootstrapResult<MountDisksOnNewHostsRequest> {
    public MountDisksOnNewHostsResult(MountDisksOnNewHostsRequest request) {
        super(request);
    }

    public MountDisksOnNewHostsResult(String statusReason, Exception errorDetails, MountDisksOnNewHostsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
