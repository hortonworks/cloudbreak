package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

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

    @PostConstruct
    public void init() {
        Joiner joiner = Joiner.on("|");
        reactor.on(Selectors.regex(joiner.join(Flow2Handler.FLOW_FINAL, Flow2Handler.FLOW_CANCEL)), flow2Handler);
        for (FlowConfiguration<?> flowConfig : flowConfigs) {
            String selector = Arrays.stream(flowConfig.getEvents()).map(FlowEvent::stringRepresentation).collect(Collectors.joining("|"));
            reactor.on(Selectors.regex(selector), flow2Handler);
        }
    }
}
