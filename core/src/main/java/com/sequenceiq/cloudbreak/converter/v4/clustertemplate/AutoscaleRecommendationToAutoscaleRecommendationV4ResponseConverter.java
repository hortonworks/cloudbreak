package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;

@Component
public class AutoscaleRecommendationToAutoscaleRecommendationV4ResponseConverter {

    public AutoscaleRecommendationV4Response convert(AutoscaleRecommendation source) {
        AutoscaleRecommendationV4Response autoscaleRecommendation = new AutoscaleRecommendationV4Response(
                source.getTimeBasedHostGroups(), source.getLoadBasedHostGroups());

        return autoscaleRecommendation;
    }
}
