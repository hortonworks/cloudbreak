package com.sequenceiq.provisioning.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.ClusterJson;
import com.sequenceiq.provisioning.domain.User;

@Service
public class AmbariClusterService {

    public void createCluster(User user, Long cloudId, ClusterJson clusterRequest) {
        // TODO: createCluser with ambari client
    }

    public List<ClusterJson> retrieveClusters(User user, Long cloudId) {
        return null;
    }

    public ClusterJson retrieveCluster(User user, Long cloudId, String id) {
        return null;
    }

}
