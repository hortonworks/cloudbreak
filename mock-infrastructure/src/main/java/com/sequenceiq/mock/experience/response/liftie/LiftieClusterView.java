package com.sequenceiq.mock.experience.response.liftie;

import com.google.gson.annotations.SerializedName;

public class LiftieClusterView {

    private String name;

    @SerializedName("cluster_id")
    private String clusterId;

    private String env;

    private String tenant;

    @SerializedName("cluster_type")
    private String clusterType;

    @SerializedName("cluster_status")
    private StatusMessage clusterStatus;

    private boolean defaultCluster;

    private boolean failCommands;

    public LiftieClusterView(String name, String clusterId, String environmentCrn, String accountId, String clusterType, StatusMessage clusterStatus,
            boolean defaultCluster) {
        this.name = name;
        this.clusterId = clusterId;
        this.env = environmentCrn;
        this.tenant = accountId;
        this.clusterType = clusterType;
        this.clusterStatus = clusterStatus;
        this.defaultCluster = defaultCluster;
    }

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

    public boolean isDefaultCluster() {
        return defaultCluster;
    }

    public void setDefaultCluster(boolean defaultCluster) {
        this.defaultCluster = defaultCluster;
    }

    public boolean isFailCommands() {
        return failCommands;
    }

    public void setFailCommands(boolean failCommands) {
        this.failCommands = failCommands;
    }
}
