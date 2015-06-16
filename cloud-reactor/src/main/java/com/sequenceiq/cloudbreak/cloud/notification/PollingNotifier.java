package com.sequenceiq.cloudbreak.cloud.notification;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.handler.PollingHandlerFactory;
import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.PollingResultNotification;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;

import reactor.bus.Event;
import reactor.fn.Pausable;
import reactor.fn.timer.Timer;

@Component
public class PollingNotifier extends EventBusAwareNotifier<DefaultPollingNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingNotifier.class);

    @Value("${cloudbreak.polling.delay.sec:10}")
    private long delay;

    @Inject
    private Timer timer;

    public void notifyPolling(DefaultPollingNotification data) {
        LOGGER.debug("Sending polling notification: {}", data);
        getEventBus().notify("polling-notification", Event.wrap(data));
    }

    public void pollingCycleDone(PollingResultNotification data) {
        LOGGER.debug("Sending polling cycle done notification: {}", data);
        getEventBus().notify("polling-cycle-done", Event.wrap(data));
    }

    public Pausable scheduleNewPollingCycle(PollingInfo pollingInfo) {
        LOGGER.debug("Scheduling new  polling cycle: {}", pollingInfo);
        Pausable pausable = timer.submit(PollingHandlerFactory.createStartPollingHandler(pollingInfo, this), delay, TimeUnit.SECONDS);
        LOGGER.debug("New polling cycle scheduled. Pausable reference: {}", pausable);
        return pausable;
    }

    public Pausable startPolling(PollingInfo pollingInfo) {
        LOGGER.debug("Scheduling new  polling cycle: {}", pollingInfo);
        Pausable pausable = timer.submit(PollingHandlerFactory.createStartPollingHandler(pollingInfo, this));
        LOGGER.debug("Polling started. Pausable reference: {}", pausable);
        return pausable;
    }

    @Override
    public void notify(DefaultPollingNotification event) {
        LOGGER.debug("Do nothing");
    }

    public void pollingInfoPersisted(PollingInfo pollingInfo) {
        LOGGER.debug("Notifying persisted polling info ready: {}", pollingInfo);
        getEventBus().notify("polling-info-ready", Event.wrap(pollingInfo));
    }
}
