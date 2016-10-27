package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StackScalingServiceTest {

    @InjectMocks
    private StackScalingService stackScalingService;

    @Mock
    private AmbariDecommissioner ambariDecommissioner;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Test
    public void shouldRemoveHostMetadataUsingId() {

        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(123L);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getInstanceId()).thenReturn("i-1234567");
        HostMetadata hostMetadata = mock(HostMetadata.class);
        when(hostMetadata.getId()).thenReturn(456L);

        stackScalingService.removeHostmetadataIfExists(stack, instanceMetaData, hostMetadata);

        verify(hostMetadataRepository).delete(456L);
    }
}
