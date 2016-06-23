package com.sequenceiq.cloudbreak.common.type

enum class ScalingType {

    DOWNSCALE_TOGETHER,
    DOWNSCALE_ONLY_STACK,
    DOWNSCALE_ONLY_CLUSTER,

    UPSCALE_TOGETHER,
    UPSCALE_ONLY_STACK,
    UPSCALE_ONLY_CLUSTER;


    companion object {

        fun isStackDownScale(scalingType: ScalingType): Boolean {
            return DOWNSCALE_TOGETHER == scalingType || DOWNSCALE_ONLY_STACK == scalingType
        }

        fun isClusterUpScale(scalingType: ScalingType): Boolean {
            return UPSCALE_TOGETHER == scalingType || UPSCALE_ONLY_CLUSTER == scalingType
        }
    }
}
