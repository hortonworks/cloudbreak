package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class RegenerateKerberosKeytabsResult extends AbstractClusterScaleResult<RegenerateKerberosKeytabsRequest> {
    public RegenerateKerberosKeytabsResult(RegenerateKerberosKeytabsRequest request) {
        super(request);
    }

    public RegenerateKerberosKeytabsResult(String statusReason, Exception errorDetails, RegenerateKerberosKeytabsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
