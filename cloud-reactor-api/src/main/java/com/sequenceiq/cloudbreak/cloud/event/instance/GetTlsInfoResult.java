package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;

public class GetTlsInfoResult extends CloudPlatformResult<CloudPlatformRequest> {

    private TlsInfo tlsInfo;

    public GetTlsInfoResult(CloudPlatformRequest<?> request, TlsInfo tlsInfo) {
        super(request);
        this.tlsInfo = tlsInfo;
    }

    public GetTlsInfoResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public TlsInfo getTlsInfo() {
        return tlsInfo;
    }
}
