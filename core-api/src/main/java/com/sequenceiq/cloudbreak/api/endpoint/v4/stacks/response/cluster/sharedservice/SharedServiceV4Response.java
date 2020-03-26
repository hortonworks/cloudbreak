package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SharedServiceV4Response implements JsonEntity {

    private String sharedClusterName;

    private Long sharedClusterId;

    private String sdxName;

    private String sdxCrn;

    private Set<AttachedClusterInfoV4Response> attachedClusters = new HashSet<>();

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

    public Set<AttachedClusterInfoV4Response> getAttachedClusters() {
        return attachedClusters;
    }

    public void setAttachedClusters(Set<AttachedClusterInfoV4Response> attachedClusters) {
        this.attachedClusters = attachedClusters;
    }

    public String getSdxName() {
        return sdxName;
    }

    public void setSdxName(String sdxName) {
        this.sdxName = sdxName;
    }

    public String getSdxCrn() {
        return sdxCrn;
    }

    public void setSdxCrn(String sdxCrn) {
        this.sdxCrn = sdxCrn;
    }
}
