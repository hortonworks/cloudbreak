package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.model.CloudbreakPolicy;
import com.sequenceiq.periscope.policies.cloudbreak.ClusterAdjustmentPolicy;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Service
public class PolicyService {

    @Autowired
    private ClusterRegistry clusterRegistry;

    public void setCloudbreakPolicy(String clusterId, CloudbreakPolicy cloudbreakPolicy) {
        Cluster cluster = clusterRegistry.get(clusterId);
        cluster.setClusterAdjustmentPolicy(new ClusterAdjustmentPolicy(cloudbreakPolicy));
    }
}
