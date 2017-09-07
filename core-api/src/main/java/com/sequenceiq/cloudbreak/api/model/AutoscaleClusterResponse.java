package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AutoscaleClusterResponse extends ClusterResponse {

    @ApiModelProperty(StackModelDescription.PASSWORD)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
