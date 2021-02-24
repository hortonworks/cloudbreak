package com.sequenceiq.sdx.api.model;

public class SdxClusterRequest extends SdxClusterRequestBase {

    private String runtime;

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    @Override
    public String toString() {
        return "SdxClusterRequest{" + "base=" + super.toString() +
                ", runtime=" + runtime +
                '}';
    }
}
