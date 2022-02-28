package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.Map;

public class DistroXScaleTestParameters {

    private static final String TIMES = "times";

    private static final String HOST_GROUP = "host_group";

    private static final String SCALE_UP_TARGET = "scale_up_target";

    private static final String SCALE_DOWN_TARGET = "scale_down_target";

    private static final String IRRELEVANT_HOST_GROUP = "irrelevant_host_group";

    private static final String DEFAULT_TIMES = "2";

    private static final String DEFAULT_SCALE_UP_TARGET = "6";

    private static final String DEFAULT_SCALE_DOWN_TARGET = "3";

    private static final String DEFAULT_HOST_GROUP = "worker";

    private static final String DEFAULT_IRRELEVANT_HOST_GROUP = "compute";

    private int times;

    private int scaleUpTarget;

    private int scaleDownTarget;

    private String hostGroup;

    private String irrelevantHostGroup;

    DistroXScaleTestParameters(Map<String, String> allParameters) {
        setHostGroup(allParameters.getOrDefault(HOST_GROUP, DEFAULT_HOST_GROUP));
        setScaleUpTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_UP_TARGET, DEFAULT_SCALE_UP_TARGET)));
        setScaleDownTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_DOWN_TARGET, DEFAULT_SCALE_DOWN_TARGET)));
        setTimes(Integer.parseInt(allParameters.getOrDefault(TIMES, DEFAULT_TIMES)));
        setIrrelevantHostGroup(allParameters.getOrDefault(IRRELEVANT_HOST_GROUP, DEFAULT_IRRELEVANT_HOST_GROUP));
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

    public String getIrrelevantHostGroup() {
        return irrelevantHostGroup;
    }

    public void setIrrelevantHostGroup(String irrelevantHostGroup) {
        this.irrelevantHostGroup = irrelevantHostGroup;
    }
}
