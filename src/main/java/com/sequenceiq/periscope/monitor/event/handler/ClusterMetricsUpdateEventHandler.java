package com.sequenceiq.periscope.monitor.event.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.ClusterMetricsUpdateEvent;
import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.service.CloudbreakService;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class ClusterMetricsUpdateEventHandler implements ApplicationListener<ClusterMetricsUpdateEvent> {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private CloudbreakService cloudbreakService;

    @Override
    public void onApplicationEvent(ClusterMetricsUpdateEvent event) {
        Cluster cluster = clusterService.get(event.getClusterId());
        cluster.updateMetrics(event.getClusterMetricsInfo());
        cloudbreakService.scale(cluster);
    }

}