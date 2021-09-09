package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ResizeRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ScaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ScaleRecommendation;

@Component
public class ScaleRecommendationToScaleRecommendationV4ResponseConverter {

    public ScaleRecommendationV4Response convert(ScaleRecommendation source) {
        AutoscaleRecommendationV4Response autoscaleRecommendation = new AutoscaleRecommendationV4Response(
                source.getAutoscaleRecommendation().getTimeBasedHostGroups(), source.getAutoscaleRecommendation().getLoadBasedHostGroups());

        ResizeRecommendationV4Response resizeRecommendation = new ResizeRecommendationV4Response(source.getResizeRecommendation().getScaleUpHostGroups(),
                source.getResizeRecommendation().getScaleDownHostGroups());

        ScaleRecommendationV4Response scaleRecommendation = new ScaleRecommendationV4Response(
                autoscaleRecommendation, resizeRecommendation);

        return scaleRecommendation;
    }
}
