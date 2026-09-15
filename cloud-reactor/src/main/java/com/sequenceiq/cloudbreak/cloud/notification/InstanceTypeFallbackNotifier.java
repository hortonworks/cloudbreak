package com.sequenceiq.cloudbreak.cloud.notification;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.notification.model.InstanceTypeFallbackEvent;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@Component
public class InstanceTypeFallbackNotifier implements InstanceTypeFallbackReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTypeFallbackNotifier.class);

    private static final int DEDUP_MAX_SIZE = 2000;

    private final Set<DedupKey> emittedKeys = ConcurrentHashMap.newKeySet();

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public void reportFallback(CloudContext cloudContext, String instanceGroup, String originalType, String fallbackType, String reasonSummary) {
        fire(cloudContext, instanceGroup, originalType, fallbackType, reasonSummary, false);
    }

    @Override
    public void reportFallbackExhausted(CloudContext cloudContext, String instanceGroup, String originalType, String reasonSummary) {
        fire(cloudContext, instanceGroup, originalType, null, reasonSummary, true);
    }

    private void fire(CloudContext cloudContext, String instanceGroup, String originalType, String fallbackType, String reasonSummary, boolean exhausted) {
        if (cloudContext == null || cloudContext.getId() == null) {
            LOGGER.debug("Skipping instance type fallback notification: cloud context or stack id is null.");
            return;
        }
        DedupKey key = new DedupKey(cloudContext.getId(), instanceGroup, originalType, fallbackType, exhausted);
        if (!emittedKeys.add(key)) {
            LOGGER.debug("Instance type fallback notification already emitted for {}, skipping duplicate.", key);
            return;
        }
        if (emittedKeys.size() > DEDUP_MAX_SIZE) {
            LOGGER.debug("Instance type fallback dedup set exceeded {} entries — clearing.", DEDUP_MAX_SIZE);
            emittedKeys.clear();
            emittedKeys.add(key);
        }
        InstanceTypeFallbackEvent event =
                new InstanceTypeFallbackEvent(cloudContext, instanceGroup, originalType, fallbackType, reasonSummary, exhausted);
        LOGGER.info("Sending instance type fallback notification: {}", event);
        eventBus.notify(InstanceTypeFallbackEvent.INSTANCE_TYPE_FALLBACK_SELECTOR, eventFactory.createEvent(event));
    }

    private record DedupKey(Long stackId, String instanceGroup, String originalType, String fallbackType, boolean exhausted) { }
}
