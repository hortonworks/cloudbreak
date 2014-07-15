package com.sequenceiq.periscope.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.ClusterMetricsUpdateRequest;

@Component
public class ClusterMonitor extends AbstractMonitor implements Monitor {

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.CLUSTER_UPDATE_RATE)
    public void update() {
        update(ClusterMetricsUpdateRequest.class);
    }
}
