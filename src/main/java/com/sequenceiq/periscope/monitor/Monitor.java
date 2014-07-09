package com.sequenceiq.periscope.monitor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public interface Monitor extends ApplicationEventPublisher, ApplicationEventPublisherAware {

    /**
     * Updates the metrics.
     */
    void update();
}
