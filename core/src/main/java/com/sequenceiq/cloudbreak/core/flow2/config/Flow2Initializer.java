package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

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
        String eventSelector = Stream.concat(Stream.of(Flow2Handler.FLOW_FINAL, Flow2Handler.FLOW_CANCEL),
                                            flowConfigs.stream().flatMap(c -> Arrays.stream(c.getEvents())).map(FlowEvent::event)
                                    ).distinct().collect(Collectors.joining("|"));
        reactor.on(Selectors.regex(eventSelector), flow2Handler);
    }
}
