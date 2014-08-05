package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class ClusterMetricsUpdateEventHandler implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMetricsUpdateEventHandler.class);

    @Autowired
    private ClusterRegistry clusterRegistry;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        ClusterMetricsInfo metrics = event.getClusterMetricsInfo();
        Cluster cluster = clusterRegistry.get(event.getClusterId());
        cluster.updateMetrics(metrics);
        int newNodeCount = cluster.scale();
        if (newNodeCount > 0) {
            int activeNodes = metrics.getActiveNodes();
            if (newNodeCount > activeNodes) {
                LOGGER.info("Should add {} new nodes with cloudbreak to {}", newNodeCount - activeNodes, event.getClusterId());
            } else if (newNodeCount < activeNodes) {
                LOGGER.info("Should remove {} nodes with cloudbreak from {}", activeNodes - newNodeCount, event.getClusterId());
            } else {
                LOGGER.info("Cluster size is optimal on cluster {}", event.getClusterId());
            }
        }
    }

}