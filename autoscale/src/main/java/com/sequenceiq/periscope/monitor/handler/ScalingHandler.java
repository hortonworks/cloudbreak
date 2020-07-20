package com.sequenceiq.periscope.monitor.handler;

import java.util.List;

import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.ScalingHandlerUtil;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class ScalingHandler implements ApplicationListener<ScalingEvent> {

    @Inject
    private ClusterService clusterService;

    @Inject
    private ScalingHandlerUtil scalingHandlerUtil;

    @Override
    public void onApplicationEvent(ScalingEvent event) {
        List<BaseAlert> alerts = event.getAlerts();
        Long clusterId = alerts.stream().findFirst().map(ma -> ma.getCluster().getId()).orElseThrow();
        Cluster cluster = clusterService.findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        alerts.forEach(alert -> {
            if (scalingHandlerUtil.isCooldownElapsed(cluster)) {
                scalingHandlerUtil.scaleIfNeed(cluster, alert);
            }
        });
    }
}