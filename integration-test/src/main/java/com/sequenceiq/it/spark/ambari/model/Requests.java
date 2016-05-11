package com.sequenceiq.it.spark.ambari.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Requests {

    private Integer id;

    @JsonProperty("request_status")
    private String requestStatus;

    @JsonProperty("progress_percent")
    private Integer progressPercent;

    public Requests(int id, String requestStatus, Integer progressPercent) {
        this.id = id;
        this.requestStatus = requestStatus;
        this.progressPercent = progressPercent;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
