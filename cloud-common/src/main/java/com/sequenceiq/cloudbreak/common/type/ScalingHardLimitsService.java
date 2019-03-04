package com.sequenceiq.cloudbreak.common.type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScalingHardLimitsService {
    private static final int DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT = 100;

    @Value("${cb.upscale.max.nodecount:" + DEFAULT_UPSCALING_MAXIMUM_NODE_COUNT + '}')
    private int maxUpscaleStepInNodeCount;

    public int getMaxUpscaleStepInNodeCount() {
        return maxUpscaleStepInNodeCount;
    }

    public boolean isViolatingMaxUpscaleStepInNodeCount(int requestedScalingAdjustment) {
        return maxUpscaleStepInNodeCount < requestedScalingAdjustment;
    }
}
