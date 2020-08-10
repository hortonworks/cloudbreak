package com.sequenceiq.periscope.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.v1.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.converter.ClusterConverter;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class ConfigurationController implements ConfigurationEndpoint {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterConverter clusterConverter;

    @Override
    public ScalingConfigurationRequest setScalingConfiguration(Long clusterId, ScalingConfigurationRequest json) {
        clusterService.updateScalingConfiguration(clusterId, json);
        return json;
    }

    @Override
    public ScalingConfigurationRequest getScalingConfiguration(Long clusterId) {
        return clusterService.getScalingConfiguration(clusterId);
    }

}
