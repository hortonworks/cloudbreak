package com.sequenceiq.environment.experience.liftie.responses;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiftieClusterView {

    private String name;

    @JsonProperty("cluster_id")
    private String clusterId;

    @JsonProperty("env")
    private String environmentCrn;

    @JsonProperty("tenant")
    private String accountId;

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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", LiftieClusterView.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("clusterId='" + clusterId + "'")
                .add("environmentCrn='" + environmentCrn + "'")
                .add("accountId='" + accountId + "'")
                .add("clusterType='" + clusterType + "'")
                .add("clusterStatus=" + clusterStatus)
                .toString();
    }
}
