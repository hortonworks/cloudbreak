package com.sequenceiq.it.spark.ambari.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class Hosts {

    @JsonProperty("host_name")
    @SerializedName("host_name")
    private String hostName;

    @JsonProperty("host_status")
    @SerializedName("host_status")
    private String hostStatus;

    public Hosts(String hostName, String hostStatus) {
        this.hostName = hostName;
        this.hostStatus = hostStatus;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostStatus() {
        return hostStatus;
    }

    public void setHostStatus(String hostStatus) {
        this.hostStatus = hostStatus;
    }
}
