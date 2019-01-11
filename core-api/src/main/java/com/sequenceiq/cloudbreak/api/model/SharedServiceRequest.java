package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SharedServiceRequest implements JsonEntity {

    private String sharedCluster;

    public String getSharedCluster() {
        return sharedCluster;
    }

    public void setSharedCluster(String sharedCluster) {
        this.sharedCluster = sharedCluster;
    }
}
