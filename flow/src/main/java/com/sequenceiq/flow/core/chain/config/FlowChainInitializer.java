package com.sequenceiq.flow.core.chain.config;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.chain.FlowChainHandler;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class FlowChainInitializer {
    @Inject
    private EventBus reactor;

    @Inject
    private FlowChainHandler flowChainHandler;

    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    @PostConstruct
    public void init() {
        flowChainFactories.stream()
                .map(FlowEventChainFactory::initEvent)
                .forEach(initEvent -> reactor.on(initEvent, flowChainHandler));
    }
}
