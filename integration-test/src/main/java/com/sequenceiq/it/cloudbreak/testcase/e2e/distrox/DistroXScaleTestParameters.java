package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.Map;

public class DistroXScaleTestParameters {

    private static final String TIMES = "times";

    private static final String HOST_GROUP = "host_group";

    private static final String SCALE_UP_TARGET = "scale_up_target";

    private static final String SCALE_DOWN_TARGET = "scale_down_target";

    private static final String DEFAULT_TIMES = "1";

    private static final String DEFAULT_SCALE_UP_TARGET = "55";

    private static final String DEFAULT_SCALE_DOWN_TARGET = "5";

    private static final String DEFAULT_HOST_GROUP = "worker";

    private int times;

    private int scaleUpTarget;

    private int scaleDownTarget;

    private String hostGroup;

    DistroXScaleTestParameters(Map<String, String> allParameters) {
        setHostGroup(allParameters.getOrDefault(HOST_GROUP, DEFAULT_HOST_GROUP));
        setScaleUpTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_UP_TARGET, DEFAULT_SCALE_UP_TARGET)));
        setScaleDownTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_DOWN_TARGET, DEFAULT_SCALE_DOWN_TARGET)));
        setTimes(Integer.parseInt(allParameters.getOrDefault(TIMES, DEFAULT_TIMES)));
    }

    public int getScaleUpTarget() {
        return scaleUpTarget;
    }

    public void setScaleUpTarget(int scaleUpTarget) {
        this.scaleUpTarget = scaleUpTarget;
    }

    public int getScaleDownTarget() {
        return scaleDownTarget;
    }

    public void setScaleDownTarget(int scaleDownTarget) {
        this.scaleDownTarget = scaleDownTarget;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
