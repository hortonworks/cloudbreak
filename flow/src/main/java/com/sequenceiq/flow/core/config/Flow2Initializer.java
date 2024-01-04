package com.sequenceiq.flow.core.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class Flow2Initializer {
    @Inject
    private EventBus reactor;

    @Inject
    private Flow2Handler flow2Handler;

    @Resource
    private List<AbstractFlowConfiguration<?, ?>> flowConfigs;

    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    @PostConstruct
    public void init() {
        Set<String> flowSelectors = new HashSet<>();
        for (AbstractFlowConfiguration<?, ?> flowConfiguration : flowConfigs) {
            for (AbstractFlowConfiguration.Transition<?, ?> transition : flowConfiguration.getTransitions()) {
                if (transition.getEvent() != null) {
                    flowSelectors.add(transition.getEvent().event());
                }
                if (transition.getFailureEvent() != null) {
                    flowSelectors.add(transition.getFailureEvent().event());
                }
            }
            flowSelectors.add(flowConfiguration.getFailHandledEvent().event());
        }
        validateNotFlowChainSelectors(flowSelectors);
        Stream.concat(Stream.of(FlowConstants.FLOW_FINAL, FlowConstants.FLOW_CANCEL), flowSelectors.stream())
                .distinct()
                .forEach(eventKey -> reactor.on(eventKey, flow2Handler));
    }

    private void validateNotFlowChainSelectors(Set<String> flowSelectors) {
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
