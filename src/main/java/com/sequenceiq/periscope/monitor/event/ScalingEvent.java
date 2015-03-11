package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.domain.BaseAlert;

public class ScalingEvent extends ApplicationEvent {

    public ScalingEvent(BaseAlert alert) {
        super(alert);
    }

    public BaseAlert getAlert() {
        return (BaseAlert) getSource();
    }

}
