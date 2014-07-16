package com.sequenceiq.periscope.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.ApplicationRequest;

@Component
public class ApplicationMonitor extends AbstractMonitor implements Monitor {

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.APP_REPORT_UPDATE_RATE)
    public void update() {
        update(ApplicationRequest.class);
    }
}
