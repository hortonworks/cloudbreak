package com.sequenceiq.periscope.monitor.event.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
public class UpdateFailedEventHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedEventHandler.class);
    private static final int RETRY_THRESHOLD = 5;

    @Autowired
    private ClusterRegistry clusterRegistry;
    private Map<String, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        String id = event.getClusterId();
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            clusterRegistry.remove(id);
            updateFailures.remove(id);
        } else {
            updateFailures.put(id, failed + 1);
        }
    }
}
