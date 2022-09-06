package com.sequenceiq.periscope.model.adjustment;

public class StopStartScalingAdjustmentParameters implements MandatoryScalingAdjustmentParameters {

    private Integer upscaleAdjustment;

    private Integer downscaleAdjustment;

    @Override
    public Integer getUpscaleAdjustment() {
        return upscaleAdjustment;
    }

    public void setUpscaleAdjustment(Integer upscaleAdjustment) {
        this.upscaleAdjustment = upscaleAdjustment;
    }

    @Override
    public Integer getDownscaleAdjustment() {
        return downscaleAdjustment;
    }

    public void setDownscaleAdjustment(Integer downscaleAdjustment) {
        this.downscaleAdjustment = downscaleAdjustment;
    }
}
