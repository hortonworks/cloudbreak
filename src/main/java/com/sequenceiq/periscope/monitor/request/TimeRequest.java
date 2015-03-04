package com.sequenceiq.periscope.monitor.request;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.EventWrapper;
import com.sequenceiq.periscope.monitor.event.TimeUpdateEvent;

@Component("TimeRequest")
@Scope("prototype")
public class TimeRequest extends AbstractEventPublisher implements Request {

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(RequestContext.CLUSTER_ID.name());
    }

    @Override
    public void run() {
        publishEvent(new EventWrapper(new TimeUpdateEvent(clusterId)));
    }
}
