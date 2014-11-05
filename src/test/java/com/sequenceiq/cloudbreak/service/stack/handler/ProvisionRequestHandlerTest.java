package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionSetupContext;

import reactor.event.Event;

public class ProvisionRequestHandlerTest {

    @InjectMocks
    private ProvisionRequestHandler underTest;

    @Mock
    private ProvisionSetupContext provisionSetupContext;

    @Mock
    private StackRepository stackRepository;

    private Event<ProvisionRequest> event;

    @Before
    public void setUp() {
        underTest = new ProvisionRequestHandler();
        MockitoAnnotations.initMocks(this);
        given(stackRepository.findById(anyLong())).willReturn(ServiceTestUtils.createStack());
        event = createEvent();
    }

    @Test
    public void testAcceptProvisionRequestEvent() {
        // GIVEN
        doNothing().when(provisionSetupContext).setupProvisioning(any(CloudPlatform.class), anyLong());
        // WHEN
        underTest.accept(event);
        // THEN
        verify(provisionSetupContext, times(1)).setupProvisioning(any(CloudPlatform.class), anyLong());
    }

    private Event<ProvisionRequest> createEvent() {
        return new Event<ProvisionRequest>(new ProvisionRequest(CloudPlatform.AZURE, 1L));
    }


}
