package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

public class SdxClusterResizeRequest {

    @NotNull
    private String environment;

    @NotNull
    private SdxClusterShape clusterShape;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    @Override
    public String toString() {
        return "SdxClusterResizeRequest{" +
                "environment='" + environment + '\'' +
                ", clusterShape=" + clusterShape +
                '}';
    }
}
