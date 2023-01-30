package com.sequenceiq.cloudbreak.common.co2;

import java.util.Map;

public class RealTimeCO2Response {

    private Map<String, RealTimeCO2> co2;

    public RealTimeCO2Response() {
    }

    public RealTimeCO2Response(Map<String, RealTimeCO2> co2) {
        this.co2 = co2;
    }

    public Map<String, RealTimeCO2> getCo2() {
        return co2;
    }

    public void setCo2(Map<String, RealTimeCO2> co2) {
        this.co2 = co2;
    }
}
