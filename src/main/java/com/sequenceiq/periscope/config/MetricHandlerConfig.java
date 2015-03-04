package com.sequenceiq.periscope.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.periscope.monitor.event.EventType;
import com.sequenceiq.periscope.monitor.handler.MetricHandler;
import com.sequenceiq.periscope.monitor.handler.TimeHandler;

@Configuration
public class MetricHandlerConfig {

    @Autowired
    private List<MetricHandler> metricHandlers;
    @Autowired
    private List<TimeHandler> timeHandlers;

    @Bean(name = "metricHandlers")
    public Map<EventType, MetricHandler> createMetricHandlers() {
        Map<EventType, MetricHandler> result = new HashMap<>();
        for (MetricHandler handler : metricHandlers) {
            result.put(handler.getEventType(), handler);
        }
        return result;
    }

}
