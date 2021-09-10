package com.sequenceiq.flow.core.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class Flow2Initializer {
    @Inject
    private EventBus reactor;

    @Inject
    private Flow2Handler flow2Handler;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    @PostConstruct
    public void init() {
        List<String> flowSelectors = flowConfigs.stream()
                .flatMap(c -> Arrays.stream(c.getEvents()))
                .map(FlowEvent::event)
                .collect(Collectors.toList());
        validateNotFlowChainSelectors(flowSelectors);
        String eventSelector = Stream.concat(Stream.of(FlowConstants.FLOW_FINAL, FlowConstants.FLOW_CANCEL), flowSelectors.stream())
                .distinct().collect(Collectors.joining("|"));
        reactor.on(Selectors.regex(eventSelector), flow2Handler);
    }

    private void validateNotFlowChainSelectors(List<String> flowSelectors) {
        Set<String> flowChainSelectors = flowChainFactories.stream()
                .map(FlowEventChainFactory::initEvent)
                .collect(Collectors.toSet());
        flowSelectors.forEach(flowSelector -> {
            if (flowChainSelectors.contains(flowSelector)) {
                throw new RuntimeException(flowSelector + " is a flow selector and a flow chain selector. It should be only in one category.");
            }
        });
    }
}
