package com.sequenceiq.periscope.monitor.event.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.registry.ClusterState;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);
    private static final int RETRY_THRESHOLD = 5;

    @Autowired
    private ClusterService clusterService;
    private Map<String, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        String id = event.getClusterId();
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            try {
                clusterService.setState(id, ClusterState.SUSPENDED);
            } catch (ClusterNotFoundException e) {
                LOGGER.warn("Trying to suspend an already deleted cluster, id: " + id, e);
            }
            updateFailures.remove(id);
        } else {
            updateFailures.put(id, failed + 1);
        }
    }
}
