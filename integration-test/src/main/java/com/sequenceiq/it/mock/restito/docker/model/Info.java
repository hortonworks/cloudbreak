package com.sequenceiq.it.mock.restito.docker.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Info {

    @SerializedName("DriverStatus")
    private List<Object> driverStatuses;

    public List<Object> getDriverStatuses() {
        return driverStatuses;
    }

    public void setDriverStatuses(List<Object> driverStatuses) {
        this.driverStatuses = driverStatuses;
    }
}
