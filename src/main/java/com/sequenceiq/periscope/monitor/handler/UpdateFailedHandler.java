package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterState;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);
    private static final int RETRY_THRESHOLD = 5;

    @Autowired
    private ClusterService clusterService;
    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long id = event.getClusterId();
        Cluster cluster = clusterService.find(id);
        MDCBuilder.buildMdcContext(cluster);
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            cluster.setState(ClusterState.SUSPENDED);
            clusterService.save(cluster);
            updateFailures.remove(id);
            LOGGER.info("Suspend cluster monitoring due to failing update attempts");
        } else {
            updateFailures.put(id, failed + 1);
        }
    }
}
