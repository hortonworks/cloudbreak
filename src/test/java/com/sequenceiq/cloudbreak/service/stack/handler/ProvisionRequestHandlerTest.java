package com.sequenceiq.cloudbreak.service.stack.handler;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionSetupContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import reactor.event.Event;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.any;

public class ProvisionRequestHandlerTest {

    @InjectMocks
    private ProvisionRequestHandler underTest;

    @Mock
    private ProvisionSetupContext provisionSetupContext;

    private Event<ProvisionRequest> event;

    @Before
    public void setUp() {
        underTest = new ProvisionRequestHandler();
        MockitoAnnotations.initMocks(this);
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
