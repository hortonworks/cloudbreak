package com.sequenceiq.cloudbreak.common.cost;

import java.util.List;

public class RealTimeCostResponse {

    private List<RealTimeCost> cost;

    public RealTimeCostResponse() {

    }

    public RealTimeCostResponse(List<RealTimeCost> cost) {
        this.cost = cost;
    }

    public List<RealTimeCost> getCost() {
        return cost;
    }

    public void setCost(List<RealTimeCost> cost) {
        this.cost = cost;
    }
}
