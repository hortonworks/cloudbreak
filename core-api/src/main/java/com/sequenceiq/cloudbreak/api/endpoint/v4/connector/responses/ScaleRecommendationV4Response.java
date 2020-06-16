package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScaleRecommendationV4Response implements JsonEntity {

    private AutoscaleRecommendationV4Response autoscaleRecommendation;

    private ResizeRecommendationV4Response resizeRecommendation;

    public ScaleRecommendationV4Response(AutoscaleRecommendationV4Response autoscaleRecommendation, ResizeRecommendationV4Response resizeRecommendation) {
        this.autoscaleRecommendation = autoscaleRecommendation;
        this.resizeRecommendation = resizeRecommendation;
    }

    public AutoscaleRecommendationV4Response getAutoscaleRecommendation() {
        return autoscaleRecommendation;
    }

    public ResizeRecommendationV4Response getResizeRecommendation() {
        return resizeRecommendation;
    }
}
