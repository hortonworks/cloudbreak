package com.sequenceiq.it.spark.ambari.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Clusters {

    @JsonProperty("cluster_name")
    private String clusterName;

    public Clusters(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
