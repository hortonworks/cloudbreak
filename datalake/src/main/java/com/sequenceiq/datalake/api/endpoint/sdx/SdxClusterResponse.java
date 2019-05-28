package com.sequenceiq.datalake.api.endpoint.sdx;

import com.sequenceiq.datalake.entity.SdxClusterStatus;

public class SdxClusterResponse {

    private String sdxCrn;

    private String sdxName;

    private SdxClusterStatus status;

    public SdxClusterResponse(String sdxCrn, String sdxName, SdxClusterStatus status) {
        this.sdxCrn = sdxCrn;
        this.sdxName = sdxName;
        this.status = status;
    }

    public String getSdxCrn() {
        return sdxCrn;
    }

    public void setSdxCrn(String sdxCrn) {
        this.sdxCrn = sdxCrn;
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
