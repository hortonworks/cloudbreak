package com.sequenceiq.cloudbreak.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScalingHardLimitsService {
    private static final int DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT = 100;

    private static final int DEFAULT_AUTOSCALING_MAXIMUM_NODE_COUNT = 500;

    @Value("${cb.upscale.max.nodecount:" + DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT + '}')
    private int maxUpscaleStepInNodeCount;

    @Value("${cb.autoscale.max.nodecount:" + DEFAULT_AUTOSCALING_MAXIMUM_NODE_COUNT + '}')
    private int maxAutoscaleStepInNodeCount;

    public int getMaxUpscaleStepInNodeCount() {
        return maxUpscaleStepInNodeCount;
    }

    public int getMaxAutoscaleStepInNodeCount() {
        return maxAutoscaleStepInNodeCount;
    }

    public boolean isViolatingMaxUpscaleStepInNodeCount(int requestedScalingAdjustment) {
        return maxUpscaleStepInNodeCount < requestedScalingAdjustment;
    }

    public boolean isViolatingAutoscaleMaxStepInNodeCount(int requestedScalingAdjustment) {
        return maxAutoscaleStepInNodeCount < requestedScalingAdjustment;
    }
}
