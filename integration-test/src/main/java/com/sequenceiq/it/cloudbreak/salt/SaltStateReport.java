package com.sequenceiq.it.cloudbreak.salt;

import java.util.Map;

public class SaltStateReport {
    private String state;

    private double totalDuration;

    private Map<String, SaltFunctionReport> functions;

    public SaltStateReport(String state, Map<String, SaltFunctionReport> functions, double totalDuration) {
        this.state = state;
        this.functions = functions;
        this.totalDuration = totalDuration;
    }

    public String getState() {
        return state;
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public Map<String, SaltFunctionReport> getFunctions() {
        return functions;
    }

    @Override
    public String toString() {
        return "SaltStateReport{" +
                "state='" + state + '\'' +
                ", functions=" + functions +
                ", totalDuration=" + totalDuration +
                '}';
    }
}
