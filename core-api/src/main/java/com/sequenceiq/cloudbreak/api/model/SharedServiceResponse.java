package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.v2.AttachedClusterInfoResponse;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SharedServiceResponse implements JsonEntity {

    private String sharedClusterName;

    private Long sharedClusterId;

    private Set<AttachedClusterInfoResponse> attachedClusters = new HashSet<>();

    public String getSharedClusterName() {
        return sharedClusterName;
    }

    public void setSharedClusterName(String sharedClusterName) {
        this.sharedClusterName = sharedClusterName;
    }

    public Long getSharedClusterId() {
        return sharedClusterId;
    }

    public void setSharedClusterId(Long sharedClusterId) {
        this.sharedClusterId = sharedClusterId;
    }

    public Set<AttachedClusterInfoResponse> getAttachedClusters() {
        return attachedClusters;
    }

    public void setAttachedClusters(Set<AttachedClusterInfoResponse> attachedClusters) {
        this.attachedClusters = attachedClusters;
    }
}
