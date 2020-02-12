package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SharedServiceV4Request implements JsonEntity {

    @ApiModelProperty(required = true)
    @NotEmpty
    private String datalakeName;

    @ApiModelProperty
    @NotEmpty
    private String runtimeVersion;

    public String getDatalakeName() {
        return datalakeName;
    }

    public void setDatalakeName(String datalakeName) {
        this.datalakeName = datalakeName;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }
}
