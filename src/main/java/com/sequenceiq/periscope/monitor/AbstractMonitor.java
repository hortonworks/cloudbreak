package com.sequenceiq.periscope.monitor;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public abstract class AbstractMonitor implements Monitor {

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void publishEvent(ApplicationEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

}
