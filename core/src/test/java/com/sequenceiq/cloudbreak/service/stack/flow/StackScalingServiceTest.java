package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;

@RunWith(MockitoJUnitRunner.class)
public class StackScalingServiceTest {

    @InjectMocks
    private StackScalingService stackScalingService;

    @Mock
    private HostMetadataService hostMetadataService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterDecomissionService clusterDecomissionService;

    @Test
    public void shouldRemoveHostMetadataUsingId() {
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        when(clusterApi.clusterDecomissionService()).thenReturn(clusterDecomissionService);

        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(123L);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getInstanceId()).thenReturn("i-1234567");
        HostMetadata hostMetadata = mock(HostMetadata.class);

        stackScalingService.removeHostmetadataIfExists(stack, instanceMetaData, hostMetadata);

        verify(hostMetadataService).delete(hostMetadata);
    }
}
