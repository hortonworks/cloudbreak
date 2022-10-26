package com.sequenceiq.cloudbreak.common.cost;

import java.util.Map;

public class EnvironmentRealTimeCostResponse {

    private Map<String, EnvironmentRealTimeCost> cost;

    public EnvironmentRealTimeCostResponse() {
    }

    public EnvironmentRealTimeCostResponse(Map<String, EnvironmentRealTimeCost> cost) {
        this.cost = cost;
    }

    public Map<String, EnvironmentRealTimeCost> getCost() {
        return cost;
    }

    public void setCost(Map<String, EnvironmentRealTimeCost> cost) {
        this.cost = cost;
    }
}
