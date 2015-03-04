package com.sequenceiq.periscope.monitor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.request.RequestContext;
import com.sequenceiq.periscope.monitor.request.TimeRequest;

@Component
public class TimeMonitor extends AbstractMonitor implements Monitor {

    @Override
    public String getIdentifier() {
        return "time-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.TIME_UPDATE_RATE_CRON;
    }

    @Override
    public Class getRequestType() {
        return TimeRequest.class;
    }

    @Override
    public Map<String, Object> getRequestContext(Cluster cluster) {
        Map<String, Object> context = new HashMap<>();
        context.put(RequestContext.CLUSTER_ID.name(), cluster.getId());
        return context;
    }
}
