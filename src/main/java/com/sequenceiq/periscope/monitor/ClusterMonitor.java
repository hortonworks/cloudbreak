package com.sequenceiq.periscope.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.ClusterMetricsRequest;

@Component
public class ClusterMonitor extends AbstractMonitor implements Monitor {

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.CLUSTER_UPDATE_RATE)
    public void update() {
        update(ClusterMetricsRequest.class);
    }
}
