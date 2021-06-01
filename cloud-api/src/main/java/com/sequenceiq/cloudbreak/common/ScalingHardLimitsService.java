package com.sequenceiq.cloudbreak.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScalingHardLimitsService {
    private static final int DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT = 100;

    private static final int DEFAULT_UPSCALING_NODE_COUNT_WHEN_SCALING_STEP_ENTITLEMENT = 200;

    @Value("${cb.upscale.max.nodecount:" + DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT + '}')
    private int maxUpscaleStepInNodeCount;

    @Value("${cb.upscale.scalingstepentitled.max.nodecount:" + DEFAULT_UPSCALING_NODE_COUNT_WHEN_SCALING_STEP_ENTITLEMENT + '}')
    private int maxUpscaleStepInNodeCountWhenScalingStepEntitled;

    public int getMaxUpscaleStepInNodeCount() {
        return maxUpscaleStepInNodeCount;
    }

    public int getMaxUpscaleStepInNodeCountWhenScalingStepEntitled() {
        return maxUpscaleStepInNodeCountWhenScalingStepEntitled;
    }

    public boolean isViolatingMaxUpscaleStepInNodeCount(int maxScaleUpStepInNodeCount, int requestedScalingAdjustment) {
        return maxScaleUpStepInNodeCount < requestedScalingAdjustment;
    }
}
