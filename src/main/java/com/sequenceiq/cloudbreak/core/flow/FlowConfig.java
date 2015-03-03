package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.core.flow.service.AmbariFlowFacade;
import com.sequenceiq.cloudbreak.core.flow.service.SimpleAmbariFlowFacade;

@Configuration
public class FlowConfig {

    @Resource
    private List<FlowHandler> flowHandlers;

    @Bean
    public Map<Class, FlowHandler> flowHandlersMap() {
        Map<Class, FlowHandler> transitions = new HashMap<>();
        for (FlowHandler handler : flowHandlers) {
            transitions.put(handler.getClass(), handler);
        }
        return transitions;
    }

    @Bean
    public AmbariFlowFacade ambariFlowFacade() {
        return new SimpleAmbariFlowFacade();
    }
}
