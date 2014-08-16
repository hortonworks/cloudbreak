package com.sequenceiq.periscope.monitor.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.ScalingService;

@Component
public class ClusterScalingHandler implements ApplicationListener<ClusterMetricsUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterScalingHandler.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScalingService scalingService;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        try {
            Cluster cluster = clusterService.get(event.getClusterId());
            cluster.updateMetrics(event.getClusterMetricsInfo());
            scalingService.scale(cluster);
        } catch (ClusterNotFoundException e) {
            LOGGER.error("Cluster not found and cannot scale, id: " + event.getClusterId(), e);
        }
    }

}