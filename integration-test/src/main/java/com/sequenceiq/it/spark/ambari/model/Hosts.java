package com.sequenceiq.it.spark.ambari.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Hosts {

    @SerializedName("host_name")
    private List<String> hostName;

    @SerializedName("host_status")
    private String hostStatus;

    public Hosts(List<String> hostName, String hostStatus) {
        this.hostName = hostName;
        this.hostStatus = hostStatus;
    }

    public List<String> getHostName() {
        return hostName;
    }

    public void setHostName(List<String> hostName) {
        this.hostName = hostName;
    }

    public String getHostStatus() {
        return hostStatus;
    }

    public void setHostStatus(String hostStatus) {
        this.hostStatus = hostStatus;
    }
}
