package com.sequenceiq.cloudbreak.service.stack.handler;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;
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

public class ProvisionCompleteHandlerTest {
    @InjectMocks
    private ProvisionCompleteHandler underTest;

    @Mock
    private MetadataSetupContext metadataSetupContext;

    private Event<ProvisionComplete> event;

    @Before
    public void setUp() {
        underTest = new ProvisionCompleteHandler();
        MockitoAnnotations.initMocks(this);
        event = createEvent();
    }

    @Test
    public void testAcceptProvisionCompleteEvent() {
        // GIVEN
        doNothing().when(metadataSetupContext).setupMetadata(any(CloudPlatform.class), anyLong());
        // WHEN
        underTest.accept(event);
        // THEN
        verify(metadataSetupContext, times(1)).setupMetadata(any(CloudPlatform.class), anyLong());
    }


    private Event<ProvisionComplete> createEvent() {
        return new Event<ProvisionComplete>(new ProvisionComplete(CloudPlatform.AWS, 1L));
    }
}
