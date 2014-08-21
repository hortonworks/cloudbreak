package com.sequenceiq.periscope.monitor;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.registry.ClusterRegistry;

public abstract class AbstractMonitor implements Monitor {

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ApplicationContext applicationContext;

    public void update(Class clazz) {
        for (Cluster cluster : clusterRegistry.getAll()) {
            if (cluster.isRunning()) {
                Runnable request = (Runnable)
                        applicationContext.getBean(clazz.getSimpleName(), cluster);
                executorService.execute(request);
            }
        }
    }
}
