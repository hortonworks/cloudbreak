package com.sequenceiq.periscope.monitor.evaluator;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public interface EventPublisher extends ApplicationEventPublisher, ApplicationEventPublisherAware, Runnable {
}
