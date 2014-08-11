package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;
import com.sequenceiq.periscope.registry.Cluster;

@Service
public class PolicyService {

    @Autowired
    private ClusterService clusterService;

    public boolean setCloudbreakPolicy(String clusterId, CloudbreakPolicy cloudbreakPolicy) {
        boolean result = false;
        Cluster cluster = clusterService.get(clusterId);
        if (cluster != null) {
            cluster.setCloudbreakPolicy(cloudbreakPolicy);
            result = true;
        }
        return result;
    }

    public CloudbreakPolicy getCloudbreakPolicy(String clusterId) {
        Cluster cluster = clusterService.get(clusterId);
        if (cluster != null) {
            return cluster.getCloudbreakPolicy();
        }
        return null;
    }

}
