package com.sequenceiq.environment.experience.liftie.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "ClusterView")
public class ClusterView {

    private String name;

    @JsonProperty("cluster_id")
    private String clusterId;

    private String env;

    private String state;

    private String tenant;

    @JsonProperty("cluster_type")
    private String clusterType;

    @JsonProperty("cluster_status")
    private StatusMessage clusterStatus;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public StatusMessage getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(StatusMessage clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

}
