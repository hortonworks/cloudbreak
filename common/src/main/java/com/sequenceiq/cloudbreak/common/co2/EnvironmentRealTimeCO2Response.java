package com.sequenceiq.cloudbreak.common.co2;

import java.util.Map;

public class EnvironmentRealTimeCO2Response {

    private Map<String, EnvironmentRealTimeCO2> co2;

    public EnvironmentRealTimeCO2Response(Map<String, EnvironmentRealTimeCO2> co2) {
        this.co2 = co2;
    }

    public Map<String, EnvironmentRealTimeCO2> getCo2() {
        return co2;
    }

    public void setCo2(Map<String, EnvironmentRealTimeCO2> co2) {
        this.co2 = co2;
    }
}
