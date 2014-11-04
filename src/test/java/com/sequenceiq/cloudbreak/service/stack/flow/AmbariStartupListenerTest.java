package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsStackUtil;

import reactor.core.Reactor;
import reactor.event.Event;

public class AmbariStartupListenerTest {
    private static final String AMBARI_IP = "172.17.0.2";
    private static final long STACK_ID = 1L;

    @InjectMocks
    @Spy
    private AmbariStartupListener underTest;

    @Mock
    private Reactor reactor;

    @Mock
    private AwsStackUtil awsStackUtil;

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private StackRepository stackRepository;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new AmbariStartupListener();
        MockitoAnnotations.initMocks(this);
        stack = createStack();
    }

    @Test
    @Ignore
    public void testWaitForAmbariServer() {
        // GIVEN
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.healthCheck()).willReturn("RUNNING");
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        // doNothing().when(awsStackUtil).sleep(anyInt());
        // WHEN
        underTest.waitForAmbariServer(STACK_ID, AMBARI_IP);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    @Ignore
    public void testWaitForAmbariServerWhenOperationTimedOut() {
        // GIVEN
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.healthCheck()).willReturn("dummyState");
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        //doNothing().when(awsStackUtil).sleep(anyInt());
        // WHEN
        underTest.waitForAmbariServer(STACK_ID, AMBARI_IP);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    @Ignore
    public void testWaitForAmbariServerWhenAmbariHealthCheckFailed() {
        // GIVEN
        doReturn(ambariClient).when(underTest).createAmbariClient(anyString());
        given(ambariClient.healthCheck()).willThrow(new IllegalStateException());
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        // doNothing().when(awsStackUtil).sleep(anyInt());
        // WHEN
        underTest.waitForAmbariServer(STACK_ID, AMBARI_IP);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setNodeCount(2);
        stack.setId(1L);
        return stack;
    }

}
