package com.sequenceiq.cloudbreak.core.flow2.chain.config;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

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
        String chainSelectors = flowChainFactories.stream().map(FlowEventChainFactory::initEvent).collect(Collectors.joining("|"));
        reactor.on(Selectors.regex(chainSelectors), flowChainHandler);
    }
}
