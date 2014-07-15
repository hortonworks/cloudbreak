package com.sequenceiq.periscope.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.SchedulerUpdateRequest;

@Component
public class SchedulerMonitor extends AbstractMonitor implements Monitor {

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.QUEUE_UPDATE_RATE)
    public void update() {
        update(SchedulerUpdateRequest.class);
    }
}
