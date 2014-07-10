package com.sequenceiq.cloudbreak.service.stack.handler;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.event.Event;

import java.util.HashMap;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.any;

public class ProvisionSetupCompleteHandlerTest {

    @InjectMocks
    private ProvisionSetupCompleteHandler underTest;

    @Mock
    private ProvisionContext provisionContext;

    private Event<ProvisionSetupComplete> event;

    @Before
    public void setUp() {
        underTest = new ProvisionSetupCompleteHandler();
        MockitoAnnotations.initMocks(this);
        event = createEvent();
    }

    @Test
    public void testAcceptProvisionSetupCompleteEvent() {
        // GIVEN
        doNothing().when(provisionContext).buildStack(any(CloudPlatform.class), anyLong(), anyMap(), anyMap());
        // WHEN
        underTest.accept(event);
        // THEN
        verify(provisionContext, times(1)).buildStack(any(CloudPlatform.class), anyLong(), anyMap(), anyMap());

    }

    private Event<ProvisionSetupComplete> createEvent() {
        ProvisionSetupComplete data = new ProvisionSetupComplete(CloudPlatform.AZURE, 1L);
        data.setSetupProperties(new HashMap<String, Object>());
        data.setUserDataParams(new HashMap<String, String>());
        return new Event<ProvisionSetupComplete>(data);
    }
}
