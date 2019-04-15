package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldFlowConfig;

import reactor.bus.EventBus;
import reactor.bus.selector.Selector;
import reactor.fn.Consumer;

public class Flow2InitializerTest {

    @InjectMocks
    private Flow2Initializer underTest;

    @Mock
    private List<FlowConfiguration<?>> flowConfigs;

    @Mock
    private EventBus reactor;

    @Mock
    private Flow2Handler flow2Handler;

    @Before
    public void setUp() {
        underTest = new Flow2Initializer();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInitialize() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        flowConfigs.add(new HelloWorldFlowConfig());
        given(this.flowConfigs.stream()).willReturn(flowConfigs.stream());
        underTest.init();
        verify(reactor, times(1)).on(any(Selector.class), any(Consumer.class));
    }
}
