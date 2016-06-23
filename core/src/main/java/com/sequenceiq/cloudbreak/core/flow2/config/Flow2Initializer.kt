package com.sequenceiq.cloudbreak.core.flow2.config

import java.util.Arrays
import java.util.stream.Collectors
import java.util.stream.Stream

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent

import reactor.bus.EventBus
import reactor.bus.selector.Selectors

@Component
class Flow2Initializer {
    @Inject
    private val reactor: EventBus? = null

    @Inject
    private val flow2Handler: Flow2Handler? = null

    @Resource
    private val flowConfigs: List<FlowConfiguration<*>>? = null

    @PostConstruct
    fun init() {
        val eventSelector = Stream.concat(Stream.of(Flow2Handler.FLOW_FINAL, Flow2Handler.FLOW_CANCEL),
                flowConfigs!!.stream().flatMap({ c -> Arrays.stream<*>(c.events) }).map(Function<*, String> { it.stringRepresentation() })).distinct().collect<String, *>(Collectors.joining("|"))
        reactor!!.on(Selectors.regex(eventSelector), flow2Handler)
    }
}
