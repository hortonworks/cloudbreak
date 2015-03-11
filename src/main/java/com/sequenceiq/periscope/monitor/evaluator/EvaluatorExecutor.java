package com.sequenceiq.periscope.monitor.evaluator;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

public interface EvaluatorExecutor extends ApplicationEventPublisher, ApplicationEventPublisherAware, Runnable {

    void setContext(Map<String, Object> context);

}
