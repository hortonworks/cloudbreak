package com.sequenceiq.flow.core.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEvent;

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
        String eventSelector = Stream.concat(Stream.of(FlowConstants.FLOW_FINAL, FlowConstants.FLOW_CANCEL),
                                            flowConfigs.stream().flatMap(c -> Arrays.stream(c.getEvents())).map(FlowEvent::event)
                                    ).distinct().collect(Collectors.joining("|"));
        reactor.on(Selectors.regex(eventSelector), flow2Handler);
    }
}
