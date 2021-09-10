package com.sequenceiq.flow.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;

import reactor.bus.EventBus;
import reactor.bus.selector.Selector;
import reactor.fn.Consumer;

@ExtendWith(MockitoExtension.class)
public class Flow2InitializerTest {

    @Mock
    private List<FlowConfiguration<?>> flowConfigs;

    @Mock
    private EventBus reactor;

    @Mock
    private Flow2Handler flow2Handler;

    @Mock
    private List<FlowEventChainFactory<?>> flowChainFactories;

    @Mock
    private FlowEventChainFactory<?> flowEventChainFactory;

    @InjectMocks
    private Flow2Initializer underTest;

    @Test
    public void testInitialize() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        flowConfigs.add(new HelloWorldFlowConfig());
        given(this.flowConfigs.stream()).willReturn(flowConfigs.stream());
        given(flowEventChainFactory.initEvent()).willReturn("OTHER_SELECTOR");
        List<FlowEventChainFactory<?>> flowChainFactories = new ArrayList<>();
        flowChainFactories.add(flowEventChainFactory);
        given(this.flowChainFactories.stream()).willReturn(flowChainFactories.stream());
        underTest.init();
        verify(reactor, times(1)).on(any(Selector.class), any(Consumer.class));
    }

    @Test
    public void testInitializeFailsWithFlowSelectorIsFlowChainSelector() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        flowConfigs.add(new HelloWorldFlowConfig());
        given(this.flowConfigs.stream()).willReturn(flowConfigs.stream());
        given(flowEventChainFactory.initEvent()).willReturn(HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT.name());
        List<FlowEventChainFactory<?>> flowChainFactories = new ArrayList<>();
        flowChainFactories.add(flowEventChainFactory);
        given(this.flowChainFactories.stream()).willReturn(flowChainFactories.stream());
        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> underTest.init());
        assertEquals("HELLOWORLD_TRIGGER_EVENT is a flow selector and a flow chain selector. It should be only in one category.",
                runtimeException.getMessage());
        verify(reactor, times(0)).on(any(Selector.class), any(Consumer.class));
    }
}
