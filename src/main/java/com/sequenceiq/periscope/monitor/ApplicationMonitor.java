package com.sequenceiq.periscope.monitor;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.ApplicationReportUpdateRequest;
import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class ApplicationMonitor implements Monitor {

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.APP_REPORT_UPDATE_RATE)
    public void update() {
        for (ClusterRegistration clusterRegistration : clusterRegistry.getAll()) {
            ApplicationReportUpdateRequest request = (ApplicationReportUpdateRequest)
                    applicationContext.getBean("applicationReportUpdateRequest", clusterRegistration);
            executorService.execute(request);
        }
    }
}
