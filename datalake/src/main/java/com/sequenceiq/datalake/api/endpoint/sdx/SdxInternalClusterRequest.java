package com.sequenceiq.datalake.api.endpoint.sdx;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;

public class SdxInternalClusterRequest extends SdxClusterRequest {

    @NotNull
    private StackV4Request stackV4Request;

    public StackV4Request getStackV4Request() {
        return stackV4Request;
    }

    public void setStackV4Request(StackV4Request stackV4Request) {
        this.stackV4Request = stackV4Request;
    }
}
