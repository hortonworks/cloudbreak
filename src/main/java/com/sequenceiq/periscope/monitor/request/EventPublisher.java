package com.sequenceiq.periscope.monitor.request;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public interface EventPublisher extends ApplicationEventPublisher, ApplicationEventPublisherAware {
}
