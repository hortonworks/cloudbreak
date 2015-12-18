package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
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
    private List<FlowConfiguration<?, ?>> flowConfigs;

    @PostConstruct
    public void init() {
        reactor.on(Selectors.regex(Flow2Handler.FLOW_FINAL), flow2Handler);
        Joiner joiner = Joiner.on("|");
        for (FlowConfiguration<?, ?> flowConfig : flowConfigs) {
            Collection<String> representations = Collections2.transform(Arrays.asList(flowConfig.getEvents()), new Function<FlowEvent, String>() {
                @Nullable @Override
                public String apply(@Nullable FlowEvent input) {
                    return input.stringRepresentation();
                }
            });
            reactor.on(Selectors.regex(joiner.join(representations)), flow2Handler);
        }
    }
}
