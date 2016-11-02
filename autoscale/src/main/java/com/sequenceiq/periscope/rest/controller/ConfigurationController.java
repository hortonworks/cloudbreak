package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class ConfigurationController implements ConfigurationEndpoint {

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ClusterConverter clusterConverter;

    @Override
    public ScalingConfigurationJson setScalingConfiguration(Long clusterId, ScalingConfigurationJson json) {
        clusterService.updateScalingConfiguration(clusterId, json);
        return json;
    }

    @Override
    public ScalingConfigurationJson getScalingConfiguration(Long clusterId) {
        return clusterService.getScalingConfiguration(clusterId);
    }

}
