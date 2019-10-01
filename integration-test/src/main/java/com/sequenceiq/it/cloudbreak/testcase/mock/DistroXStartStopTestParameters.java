package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Map;

class DistroXStartStopTestParameters {

    private static final String MAX = "max";

    private static final String MIN = "min";

    private static final String TIMES = "times";

    private static final String HOSTGROUP = "hostgroup";

    private static final int DEFAULT_MAX_VALUE = 10;

    private static final int DEFAULT_MIN_VALUE = 5;

    private static final int DEFAULT_TIMES = 4;

    private static final String DEFAULT_HOSTGROUP = "worker";

    private int min;

    private int max;

    private int times;

    private String hostgroup;

    private DistroXStartStopTestParameters() {
    }

    DistroXStartStopTestParameters(Map<String, String> allParameters) {
        String max = allParameters.get(MAX);
        String min = allParameters.get(MIN);
        String times = allParameters.get(TIMES);
        String hostGroup = allParameters.get(HOSTGROUP);

        setHostgroup(hostGroup == null ? DEFAULT_HOSTGROUP : hostGroup);
        setMax(max == null ? DEFAULT_MAX_VALUE : Integer.parseInt(max));
        setMin(min == null ? DEFAULT_MIN_VALUE : Integer.parseInt(min));
        setTimes(times == null ? DEFAULT_TIMES : Integer.parseInt(times));
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public String getHostgroup() {
        return hostgroup;
    }

    public void setHostgroup(String hostgroup) {
        this.hostgroup = hostgroup;
    }
}
