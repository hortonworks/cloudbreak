package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariRegenerateKerberosKeytabsResult extends AbstractClusterScaleResult<AmbariRegenerateKerberosKeytabsRequest> {
    public AmbariRegenerateKerberosKeytabsResult(AmbariRegenerateKerberosKeytabsRequest request) {
        super(request);
    }

    public AmbariRegenerateKerberosKeytabsResult(String statusReason, Exception errorDetails, AmbariRegenerateKerberosKeytabsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
