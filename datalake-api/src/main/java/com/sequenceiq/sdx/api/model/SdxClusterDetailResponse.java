package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;

public class SdxClusterDetailResponse extends SdxClusterResponse {

    private StackV4Response stackV4Response;

    public SdxClusterDetailResponse() {
    }

    public SdxClusterDetailResponse(SdxClusterResponse sdxClusterResponse, StackV4Response stackV4Response) {
        super(sdxClusterResponse.getCrn(), sdxClusterResponse.getName(), sdxClusterResponse.getStatus(),
                sdxClusterResponse.getStatusReason(), sdxClusterResponse.getEnvironmentName(), sdxClusterResponse.getEnvironmentCrn());
        this.stackV4Response = stackV4Response;
    }

    public StackV4Response getStackV4Response() {
        return stackV4Response;
    }

    public void setStackV4Response(StackV4Response stackV4Response) {
        this.stackV4Response = stackV4Response;
    }
}
