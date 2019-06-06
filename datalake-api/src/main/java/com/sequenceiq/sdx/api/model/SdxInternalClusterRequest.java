package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;

public class SdxInternalClusterRequest extends SdxClusterRequest {

    private StackV4Request stackV4Request;

    public StackV4Request getStackV4Request() {
        return stackV4Request;
    }

    public void setStackV4Request(StackV4Request stackV4Request) {
        this.stackV4Request = stackV4Request;
    }
}
