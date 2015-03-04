package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(UpdateFailedHandler.class);
    private static final int RETRY_THRESHOLD = 5;

    @Autowired
    private ClusterService clusterService;
    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long id = event.getClusterId();
        try {
            Cluster cluster = clusterService.get(id);
            Integer failed = updateFailures.get(id);
            if (failed == null) {
                updateFailures.put(id, 1);
            } else if (RETRY_THRESHOLD - 1 == failed) {
                cluster.setState(ClusterState.SUSPENDED);
                updateFailures.remove(id);
            } else {
                updateFailures.put(id, failed + 1);
            }
        } catch (ClusterNotFoundException e) {
            LOGGER.warn(id, "Trying to suspend an already deleted cluster", e);
        }
    }
}
