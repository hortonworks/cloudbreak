package com.sequenceiq.cloudbreak.service.stack.handler;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.domain.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.event.Event;

import java.util.HashSet;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySetOf;

public class MetadataSetupCompleteHandlerTest {
    @InjectMocks
    private MetadataSetupCompleteHandler underTest;

    @Mock
    private AmbariRoleAllocator ambariRoleAllocator;

    private Event<MetadataSetupComplete> event;

    @Before
    public void setUp() {
        underTest = new MetadataSetupCompleteHandler();
        MockitoAnnotations.initMocks(this);
        event = createEvent();
    }

    @Test
    public void testAcceptMetadataSetupCompleteEvent() {
        // GIVEN
        doNothing().when(ambariRoleAllocator).allocateRoles(anyLong(), anySetOf(CoreInstanceMetaData.class));
        // WHEN
        underTest.accept(event);
        // THEN
        verify(ambariRoleAllocator, times(1)).allocateRoles(anyLong(), anySetOf(CoreInstanceMetaData.class));
    }

    private Event<MetadataSetupComplete> createEvent() {
        return new Event<MetadataSetupComplete>(
                new MetadataSetupComplete(CloudPlatform.AZURE, 1L, new HashSet<CoreInstanceMetaData>()));
    }
}
