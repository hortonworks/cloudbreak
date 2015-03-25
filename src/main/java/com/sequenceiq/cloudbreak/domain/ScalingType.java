package com.sequenceiq.cloudbreak.domain;

public enum ScalingType {

    DOWNSCALE_TOGETHER,
    DOWNSCALE_ONLY_STACK,
    DOWNSCALE_ONLY_CLUSTER,

    UPSCALE_TOGETHER,
    UPSCALE_ONLY_STACK,
    UPSCALE_ONLY_CLUSTER;

    public static boolean isStackDownScale(ScalingType scalingType) {
        return DOWNSCALE_TOGETHER.equals(scalingType) || DOWNSCALE_ONLY_STACK.equals(scalingType);
    }

    public static boolean isClusterUpScale(ScalingType scalingType) {
        return UPSCALE_TOGETHER.equals(scalingType) || UPSCALE_ONLY_CLUSTER.equals(scalingType);
    }
}
