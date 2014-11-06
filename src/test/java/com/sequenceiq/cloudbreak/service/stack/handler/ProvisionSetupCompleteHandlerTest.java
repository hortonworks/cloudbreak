package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisionContext;

import reactor.event.Event;

public class ProvisionSetupCompleteHandlerTest {

    @InjectMocks
    private ProvisionSetupCompleteHandler underTest;

    @Mock
    private ProvisionContext provisionContext;

    @Mock
    private RetryingStackUpdater retryingStackUpdater;

    @Mock
    private StackRepository stackRepository;

    private Event<ProvisionSetupComplete> event;

    @Before
    public void setUp() {
        underTest = new ProvisionSetupCompleteHandler();
        MockitoAnnotations.initMocks(this);
        given(stackRepository.findById(anyLong())).willReturn(ServiceTestUtils.createStack());
        event = createEvent();
    }

    @Test
    public void testAcceptProvisionSetupCompleteEvent() {
        // GIVEN
        doNothing().when(provisionContext).buildStack(any(CloudPlatform.class), anyLong(), anyMap(), anyMap());
        given(retryingStackUpdater.updateStackResources(any(Long.class), anySet())).willReturn(null);
        // WHEN
        underTest.accept(event);
        // THEN
        verify(provisionContext, times(1)).buildStack(any(CloudPlatform.class), anyLong(), anyMap(), anyMap());

    }

    private Event<ProvisionSetupComplete> createEvent() {
        ProvisionSetupComplete data = new ProvisionSetupComplete(CloudPlatform.AZURE, 1L);
        data.setSetupProperties(new HashMap<String, Object>());
        data.setUserDataParams(new HashMap<String, String>());
        return new Event<>(data);
    }
}
