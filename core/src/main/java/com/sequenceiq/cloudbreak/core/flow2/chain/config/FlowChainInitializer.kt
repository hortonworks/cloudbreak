package com.sequenceiq.cloudbreak.core.flow2.chain.config

import java.util.stream.Collectors

import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler

import reactor.bus.EventBus
import reactor.bus.selector.Selectors

@Component
class FlowChainInitializer {
    @Inject
    private val reactor: EventBus? = null
    @Inject
    private val flowChainHandler: FlowChainHandler? = null
    @Resource
    private val flowChainFactories: List<FlowEventChainFactory<Payload>>? = null

    @PostConstruct
    fun init() {
        val chainSelectors = flowChainFactories!!.stream().map(Function<FlowEventChainFactory, String> { it.initEvent() }).collect(Collectors.joining("|"))
        reactor!!.on(Selectors.regex(chainSelectors), flowChainHandler)
    }
}
