package com.sequenceiq.periscope.monitor.handler;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.monitor.event.EventType;

public interface MetricHandler {

    List<MetricResult> isAlarmHit(ApplicationEvent event);

    EventType getEventType();
}
