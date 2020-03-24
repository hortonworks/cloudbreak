package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.Map;

public class DistroXScaleTestParameters {

    private static final String TIMES = "times";

    private static final String HOSTGROUP = "hostgroup";

    private static final String SCALE_UP = "scale_up";

    private static final String SCALE_DOWN = "scale_down";

    private static final int DEFAULT_TIMES = 10;

    private static final int DEFAULT_SCALE_UP = 100;

    private static final int DEFAULT_SCALE_DOWN = 10;

    private static final String DEFAULT_HOSTGROUP = "worker";

    private int times;

    private int scaleUp;

    private int scaleDown;

    private String hostgroup;

    private DistroXScaleTestParameters() {
    }

    DistroXScaleTestParameters(Map<String, String> allParameters) {
        String times = allParameters.get(TIMES);
        String scaleUp = allParameters.get(SCALE_UP);
        String scaleDown = allParameters.get(SCALE_DOWN);
        String hostGroup = allParameters.get(HOSTGROUP);

        setHostgroup(hostGroup == null ? DEFAULT_HOSTGROUP : hostGroup);
        setScaleUp(scaleUp == null ? DEFAULT_SCALE_UP : Integer.parseInt(scaleUp));
        setScaleDown(scaleDown == null ? DEFAULT_SCALE_DOWN : Integer.parseInt(scaleDown));
        setTimes(times == null ? DEFAULT_TIMES : Integer.parseInt(times));
    }

    public int getScaleUp() {
        return scaleUp;
    }

    public void setScaleUp(int scaleUp) {
        this.scaleUp = scaleUp;
    }

    public int getScaleDown() {
        return scaleDown;
    }

    public void setScaleDown(int scaleDown) {
        this.scaleDown = scaleDown;
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
