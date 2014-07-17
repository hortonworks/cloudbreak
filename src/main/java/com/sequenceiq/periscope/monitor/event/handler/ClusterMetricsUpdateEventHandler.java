package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class ClusterMetricsUpdateEventHandler implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMetricsUpdateEventHandler.class);
    private static final double FREE_RESOURCE_RATE_THRESHOLD_LOW = 0.2;
    private static final double FREE_RESOURCE_RATE_THRESHOLD_HIGH = 0.4;

    @Value("${node.count.min:3}")
    private int minNodeCount;

    @Autowired
    private ClusterRegistry clusterRegistry;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        ClusterMetricsInfo metrics = event.getClusterMetricsInfo();
        Cluster cluster = clusterRegistry.get(event.getClusterId());
        cluster.updateMetrics(metrics);
        if (shouldAddNewNodes(metrics)) {
            LOGGER.info("Should add new nodes with cloudbreak to {}", event.getClusterId());
        } else if (shouldRemoveNodes(metrics)) {
            LOGGER.info("Should remove nodes with cloudbreak from {}", event.getClusterId());
        } else {
            LOGGER.info("Cluster size is optimal on cluster {}", event.getClusterId());
        }
    }

    private boolean shouldRemoveNodes(ClusterMetricsInfo metrics) {
        return isAboveThreshold(metrics) && metrics.getActiveNodes() > minNodeCount;
    }

    private boolean isAboveThreshold(ClusterMetricsInfo metrics) {
        return computeFreeResourceRate(metrics) > FREE_RESOURCE_RATE_THRESHOLD_HIGH;
    }

    private boolean shouldAddNewNodes(ClusterMetricsInfo metrics) {
        return isBelowThreshold(metrics);
    }

    private boolean isBelowThreshold(ClusterMetricsInfo metrics) {
        return computeFreeResourceRate(metrics) < FREE_RESOURCE_RATE_THRESHOLD_LOW;
    }

    private double computeFreeResourceRate(ClusterMetricsInfo metrics) {
        return (double) metrics.getAvailableMB() / (double) metrics.getTotalMB();
    }
}
