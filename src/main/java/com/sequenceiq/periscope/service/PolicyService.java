package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.policies.scaling.ScalingPolicy;

@Service
public class PolicyService {

    @Autowired
    private ClusterService clusterService;

    public void setScalingPolicy(String clusterId, ScalingPolicy scalingPolicy) throws ClusterNotFoundException {
        clusterService.get(clusterId).setScalingPolicy(scalingPolicy);
    }

    public ScalingPolicy getScalingPolicy(String clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getScalingPolicy();
    }

}
