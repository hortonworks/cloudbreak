package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.domain.BaseAlert;

public class ScalingEvent extends ApplicationEvent {

    public ScalingEvent(BaseAlert alert) {
        this(List.of(alert));
    }

    public ScalingEvent(List<? extends BaseAlert> alerts) {
        super(alerts);
    }

    public List<BaseAlert> getAlerts() {
        return (List<BaseAlert>) getSource();
    }

}
