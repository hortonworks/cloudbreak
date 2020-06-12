package com.sequenceiq.periscope.service;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@Service
public class AutoscaleRecommendationService {

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Cacheable(cacheNames = "autoscaleRecommendationCache", key = "{#clusterCrn}")
    public AutoscaleRecommendationV4Response getAutoscaleRecommendations(String clusterCrn) {
        return cloudbreakCommunicator.getRecommendationForCluster(clusterCrn);
    }
}
