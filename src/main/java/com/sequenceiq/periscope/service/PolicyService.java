package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.policies.cloudbreak.CloudbreakPolicy;

@Service
public class PolicyService {

    @Autowired
    private ClusterService clusterService;

    public void setCloudbreakPolicy(String clusterId, CloudbreakPolicy cloudbreakPolicy) throws ClusterNotFoundException {
        clusterService.get(clusterId).setCloudbreakPolicy(cloudbreakPolicy);
    }

    public CloudbreakPolicy getCloudbreakPolicy(String clusterId) throws ClusterNotFoundException {
        return clusterService.get(clusterId).getCloudbreakPolicy();
    }

}
