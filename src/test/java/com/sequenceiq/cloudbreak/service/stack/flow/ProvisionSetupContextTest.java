package com.sequenceiq.cloudbreak.service.stack.flow;


import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

import reactor.core.Reactor;
import reactor.event.Event;

public class ProvisionSetupContextTest {
    @InjectMocks
    private ProvisionSetupContext underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ProvisionSetup provisionSetup;

    @Mock
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Mock
    private Reactor reactor;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new ProvisionSetupContext();
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
    }

    @Test
    public void testSetupProvisioning() {
        // GIVEN
        given(provisionSetups.get(CloudPlatform.AWS)).willReturn(provisionSetup);
        given(stackRepository.findById(1L)).willReturn(stack);
        doNothing().when(provisionSetup).setupProvisioning(stack);
        // WHEN
        underTest.setupProvisioning(CloudPlatform.AWS, 1L);
        // THEN
        verify(provisionSetup, times(1)).setupProvisioning(stack);
        verify(reactor, times(0)).notify(any(ReactorConfig.class), any(Event.class));
    }

    @Test
    public void testSetupProvisioningWhenExceptionOccursShouldNotify() {
        // GIVEN
        given(provisionSetups.get(CloudPlatform.AWS)).willReturn(provisionSetup);
        given(stackRepository.findById(1L)).willReturn(stack);
        given(stackRepository.findOneWithLists(1L)).willReturn(stack);
        doThrow(new IllegalStateException()).when(provisionSetup).setupProvisioning(stack);
        // WHEN
        underTest.setupProvisioning(CloudPlatform.AWS, 1L);
        // THEN
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

}
