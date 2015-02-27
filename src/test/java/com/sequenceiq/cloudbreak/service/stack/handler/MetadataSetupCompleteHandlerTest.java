package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.event.Event;

@Ignore("Rewrite this test for the new flow / eventually delete it!!!")
public class MetadataSetupCompleteHandlerTest {
    @InjectMocks
    private MetadataSetupCompleteHandler underTest;

    @Mock
    private AmbariRoleAllocator ambariRoleAllocator;

    @Mock
    private StackRepository stackRepository;

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
        given(stackRepository.findById(anyLong())).willReturn(ServiceTestUtils.createStack());
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
