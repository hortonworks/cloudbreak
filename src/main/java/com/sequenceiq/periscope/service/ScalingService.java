package com.sequenceiq.periscope.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.registry.Cluster;

@Service
public class ScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingService.class);

    public void scale(Cluster cluster) {
        int desiredNodeCount = cluster.scale();
        if (desiredNodeCount > 0) {
            int totalNodes = cluster.getTotalNodes();
            if (desiredNodeCount > totalNodes) {
                scaleUpTo(cluster, desiredNodeCount);
            } else if (desiredNodeCount < totalNodes) {
                scaleDownTo(cluster, desiredNodeCount);
            } else {
                LOGGER.info("Cluster size is optimal on {}", cluster.getId());
            }
        } else {
            LOGGER.info("No scaling activity on {}", cluster.getId());
        }
    }

    public void scaleUpTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should add {} new nodes with cloudbreak to {}", nodeCount - cluster.getTotalNodes(), cluster.getId());
    }

    public void scaleDownTo(Cluster cluster, int nodeCount) {
        LOGGER.info("Should remove {} nodes with cloudbreak from {}", cluster.getTotalNodes() - nodeCount, cluster.getId());
    }
}
