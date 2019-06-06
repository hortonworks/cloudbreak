package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;

public class GetTlsInfoResult extends CloudPlatformResult {

    private TlsInfo tlsInfo;

    public GetTlsInfoResult(Long resourceId, TlsInfo tlsInfo) {
        super(resourceId);
        this.tlsInfo = tlsInfo;
    }

    public GetTlsInfoResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public TlsInfo getTlsInfo() {
        return tlsInfo;
    }
}
