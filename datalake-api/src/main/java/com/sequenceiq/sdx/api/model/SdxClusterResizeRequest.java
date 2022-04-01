package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterResizeRequest {

    @ApiModelProperty(ModelDescriptions.ENVIRONMENT_NAME)
    @NotNull
    private String environment;

    @ApiModelProperty(ModelDescriptions.CLUSTER_SHAPE)
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
