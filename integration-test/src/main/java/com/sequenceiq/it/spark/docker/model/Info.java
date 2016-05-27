package com.sequenceiq.it.spark.docker.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Info {

    @SerializedName("DriverStatus")
    private List<Object> driverStatuses;

    public Info() {
    }

    public Info(int serverNumber) {
        driverStatuses = createStatusList(serverNumber);
    }

    private static List<Object> createStatusList(int serverNumber) {
        List<Object> statusList = new ArrayList<>();
        for (int i = 0; i <= serverNumber / 254; i++) {
            int subAddress = Integer.min(254, serverNumber - i * 254);
            for (int j = 1; j <= subAddress; j++) {
                List<String> ipList = new ArrayList<>();
                ipList.add("server");
                ipList.add("192.168." + i + "." + j);
                statusList.add(ipList);
            }
        }
        return statusList;
    }

    public List<Object> getDriverStatuses() {
        return driverStatuses;
    }

    public void setDriverStatuses(List<Object> driverStatuses) {
        this.driverStatuses = driverStatuses;
    }
}
