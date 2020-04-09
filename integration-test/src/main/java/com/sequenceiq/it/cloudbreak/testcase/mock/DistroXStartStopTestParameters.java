package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Map;

class DistroXStartStopTestParameters {

    private static final String TIMES = "times";

    private static final String HOSTGROUP = "hostgroup";

    private static final String STEP = "step";

    private static final int DEFAULT_TIMES = 16;

    private static final int DEFAULT_STEP = 100;

    private static final String DEFAULT_HOSTGROUP = "worker";

    private int times;

    private int step;

    private String hostgroup;

    private DistroXStartStopTestParameters() {
    }

    DistroXStartStopTestParameters(Map<String, String> allParameters) {
        String times = allParameters.get(TIMES);
        String step = allParameters.get(STEP);
        String hostGroup = allParameters.get(HOSTGROUP);

        setHostgroup(hostGroup == null ? DEFAULT_HOSTGROUP : hostGroup);
        setStep(step == null ? DEFAULT_STEP : Integer.parseInt(step));
        setTimes(times == null ? DEFAULT_TIMES : Integer.parseInt(times));
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
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
