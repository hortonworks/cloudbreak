package com.sequenceiq.periscope.monitor;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.request.QueueInfoUpdateRequest;
import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class QueueMonitor implements Monitor {

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    @Scheduled(fixedRate = MonitorUpdateRate.QUEUE_UPDATE_RATE)
    public void update() {
        for (ClusterRegistration clusterRegistration : clusterRegistry.getAll()) {
            QueueInfoUpdateRequest request = (QueueInfoUpdateRequest)
                    applicationContext.getBean(QueueInfoUpdateRequest.class.getSimpleName(), clusterRegistration);
            executorService.execute(request);
        }
    }

}
