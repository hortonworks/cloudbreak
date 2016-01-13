package com.sequenceiq.periscope.monitor.evaluator;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public abstract class AbstractEventPublisher implements EventPublisher {

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void publishEvent(Object event) {
        this.eventPublisher.publishEvent(event);
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        this.eventPublisher.publishEvent(event);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }
}
