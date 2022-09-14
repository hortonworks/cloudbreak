package com.sequenceiq.cloudbreak.common.cost;

import java.util.Map;

public class RealTimeCostResponse {

    private Map<String, RealTimeCost> cost;

    public RealTimeCostResponse() {

    }

    public RealTimeCostResponse(Map<String, RealTimeCost> cost) {
        this.cost = cost;
    }

    public Map<String, RealTimeCost> getCost() {
        return cost;
    }

    public void setCost(Map<String, RealTimeCost> cost) {
        this.cost = cost;
    }
}
