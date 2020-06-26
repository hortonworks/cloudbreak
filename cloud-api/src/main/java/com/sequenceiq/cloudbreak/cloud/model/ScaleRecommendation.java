package com.sequenceiq.cloudbreak.cloud.model;

public class ScaleRecommendation {

    private AutoscaleRecommendation autoscaleRecommendation;

    private ResizeRecommendation resizeRecommendation;

    public ScaleRecommendation(AutoscaleRecommendation autoscaleRecommendation, ResizeRecommendation resizeRecommendation) {
        this.autoscaleRecommendation = autoscaleRecommendation;
        this.resizeRecommendation = resizeRecommendation;
    }

    public AutoscaleRecommendation getAutoscaleRecommendation() {
        return autoscaleRecommendation;
    }

    public ResizeRecommendation getResizeRecommendation() {
        return resizeRecommendation;
    }
}
