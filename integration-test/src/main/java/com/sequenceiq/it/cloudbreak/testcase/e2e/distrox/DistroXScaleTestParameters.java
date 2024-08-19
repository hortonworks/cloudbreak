package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.Map;

import org.apache.commons.lang3.EnumUtils;

import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroXScaleTestParameters {

    private static final String TIMES = "times";

    private static final String HOST_GROUP = "host_group";

    private static final String SCALE_UP_TARGET = "scale_up_target";

    private static final String AWS_SCALING_TIME = "aws_scaling_time";

    private static final String AZURE_SCALING_TIME = "azure_scaling_time";

    private static final String GCP_SCALING_TIME = "gcp_scaling_time";

    private static final String SCALE_DOWN_TARGET = "scale_down_target";

    private static final String IRRELEVANT_HOST_GROUP = "irrelevant_host_group";

    private static final String ADJUSTMENT_TYPE = "adjustment_type";

    private static final String THRESHOLD = "threshold";

    private static final String DEFAULT_TIMES = "2";

    private static final String DEFAULT_SCALING_TIME = "6";

    private static final String DEFAULT_SCALE_UP_TARGET = "6";

    private static final String DEFAULT_SCALE_DOWN_TARGET = "3";

    private static final String DEFAULT_HOST_GROUP = "worker";

    private static final String DEFAULT_IRRELEVANT_HOST_GROUP = "compute";

    private static final String DEFAULT_ADJUSTMENT_TYPE = "EXACT";

    private static final String DEFAULT_THRESHOLD = "0";

    private int times;

    private long awsScalingTime;

    private long azureScalingTime;

    private long gcpScalingTime;

    private int scaleUpTarget;

    private int scaleDownTarget;

    private String hostGroup;

    private String irrelevantHostGroup;

    private AdjustmentType adjustmentType;

    private long threshold;

    DistroXScaleTestParameters(Map<String, String> allParameters) {
        setHostGroup(allParameters.getOrDefault(HOST_GROUP, DEFAULT_HOST_GROUP));
        setScaleUpTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_UP_TARGET, DEFAULT_SCALE_UP_TARGET)));
        setScaleDownTarget(Integer.parseInt(allParameters.getOrDefault(SCALE_DOWN_TARGET, DEFAULT_SCALE_DOWN_TARGET)));
        setTimes(Integer.parseInt(allParameters.getOrDefault(TIMES, DEFAULT_TIMES)));
        setIrrelevantHostGroup(allParameters.getOrDefault(IRRELEVANT_HOST_GROUP, DEFAULT_IRRELEVANT_HOST_GROUP));
        setAdjustmentType(allParameters.getOrDefault(ADJUSTMENT_TYPE, DEFAULT_ADJUSTMENT_TYPE));
        setThreshold(Long.parseLong(allParameters.getOrDefault(THRESHOLD, DEFAULT_THRESHOLD)));
        setAwsScalingTime(Long.parseLong(allParameters.getOrDefault(AWS_SCALING_TIME, DEFAULT_SCALING_TIME)));
        setAzureScalingTime(Long.parseLong(allParameters.getOrDefault(AZURE_SCALING_TIME, DEFAULT_SCALING_TIME)));
        setGcpScalingTime(Long.parseLong(allParameters.getOrDefault(GCP_SCALING_TIME, DEFAULT_SCALING_TIME)));
    }

    public int getScaleUpTarget() {
        return scaleUpTarget;
    }

    public long getAwsScalingTime() {
        return awsScalingTime;
    }

    public long getAzureScalingTime() {
        return azureScalingTime;
    }

    public long getGcpScalingTime() {
        return gcpScalingTime;
    }

    public void setScaleUpTarget(int scaleUpTarget) {
        this.scaleUpTarget = scaleUpTarget;
    }

    public void setAwsScalingTime(long awsScalingTime) {
        this.awsScalingTime = awsScalingTime;
    }

    public void setGcpScalingTime(long gcpScalingTime) {
        this.gcpScalingTime = gcpScalingTime;
    }

    public void setAzureScalingTime(long azureScalingTime) {
        this.azureScalingTime = azureScalingTime;
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

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentTypeName) {
        if (EnumUtils.isValidEnum(AdjustmentType.class, adjustmentTypeName)) {
            adjustmentType = EnumUtils.getEnum(AdjustmentType.class, adjustmentTypeName);
        } else {
            throw new TestFailException(String.format("There is no scaling AdjustmentType for '%s'", adjustmentTypeName));
        }
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }
}
