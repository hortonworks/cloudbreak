package com.sequenceiq.periscope.monitor.request;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public interface Request extends ApplicationEventPublisher, ApplicationEventPublisherAware, Runnable {

    void setContext(Map<String, Object> context);

}
