package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class ClusterResponse {

    private Long id;
    private String status;
    private int hoursUp;
    private int minutesUp;
    private String cluster;
    private Long blueprintId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getHoursUp() {
        return hoursUp;
    }

    public void setHoursUp(int hoursUp) {
        this.hoursUp = hoursUp;
    }

    public int getMinutesUp() {
        return minutesUp;
    }

    public void setMinutesUp(int minutesUp) {
        this.minutesUp = minutesUp;
    }

    @JsonRawValue
    public String getCluster() {
        return cluster;
    }

    public void setCluster(JsonNode node) {
        this.cluster = node.toString();
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }
}
