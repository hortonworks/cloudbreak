package com.sequenceiq.datalake.api.endpoint.sdx;

import com.sequenceiq.datalake.entity.SdxClusterStatus;

public class SdxClusterResponse {

    private String sdxName;

    private SdxClusterStatus status;

    public SdxClusterResponse(String sdxName, SdxClusterStatus status) {
        this.sdxName = sdxName;
        this.status = status;
    }

    public SdxClusterStatus getStatus() {
        return status;
    }

    public void setStatus(SdxClusterStatus status) {
        this.status = status;
    }

    public String getSdxName() {
        return sdxName;
    }

    public void setSdxName(String sdxName) {
        this.sdxName = sdxName;
    }
}
