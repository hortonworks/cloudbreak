package com.sequenceiq.periscope.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.registry.Cluster;

@Service
public class CloudbreakService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakService.class);

    public void scale(Cluster cluster) {
        int newNodeCount = cluster.scale();
        if (newNodeCount > 0) {
            int totalNodes = cluster.getTotalNodes();
            if (newNodeCount > totalNodes) {
                scaleUpTo(cluster, newNodeCount);
            } else if (newNodeCount < totalNodes) {
                scaleDownTo(cluster, newNodeCount);
            } else {
                LOGGER.info("Cluster size is optimal on cluster {}", cluster.getId());
            }
        }
    }

    public void scaleUpTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should add {} new nodes with cloudbreak to {}", nodeCount - cluster.getTotalNodes(), cluster.getId());
    }

    public void scaleDownTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should remove {} nodes with cloudbreak from {}", cluster.getTotalNodes() - nodeCount, cluster.getId());
    }
}
