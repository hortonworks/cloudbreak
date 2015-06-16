package com.sequenceiq.cloudbreak.cloud.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

import reactor.bus.Event;


@Component
public class PollingInfoNotifier extends EventBusAwareNotifier<PollingInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingInfoNotifier.class);

    @Override
    public void notify(PollingInfo data) {
        LOGGER.debug("Sending polling info ready notification : {}", data);
        getEventBus().notify("polling-info-ready", Event.wrap(data));
    }
}
