package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
class SeLinuxEnablementServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private Stack stack;

    @InjectMocks
    private SeLinuxEnablementService underTest;

    @Test
    void testEnableSeLinuxOnAllNodes() throws CloudbreakOrchestratorException {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getPrivateIp()).thenReturn("0.0.0.0");
        when(instanceMetaData.getPublicIpWrapper()).thenReturn("public-wrapper");
        when(instanceMetaData.getInstanceId()).thenReturn("test-instance-id");
        InstanceGroup ig = mock(InstanceGroup.class);
        Template template = mock(Template.class);
        when(template.getInstanceType()).thenReturn("test-instance-type");
        when(ig.getTemplate()).thenReturn(template);
        when(ig.getGroupName()).thenReturn("test-group");
        when(instanceMetaData.getInstanceGroup()).thenReturn(ig);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("test");
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getId()).thenReturn(1L);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        underTest.modifySeLinuxOnAllNodes(stack);
        verify(hostOrchestrator).enableSeLinuxOnNodes(any(), any(), any());
    }

    @Test
    void testEnableSeLinuxOnAllNodesNoNodeException() {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> underTest.modifySeLinuxOnAllNodes(stack));
        assertEquals("There are no nodes while scanning instance metadata.", exception.getMessage());
    }

    @Test
    void testEnableSeLinuxOnAllNodesFailed() throws CloudbreakOrchestratorException {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getPrivateIp()).thenReturn("0.0.0.0");
        when(instanceMetaData.getPublicIpWrapper()).thenReturn("public-wrapper");
        when(instanceMetaData.getInstanceId()).thenReturn("test-instance-id");
        InstanceGroup ig = mock(InstanceGroup.class);
        Template template = mock(Template.class);
        when(template.getInstanceType()).thenReturn("test-instance-type");
        when(ig.getTemplate()).thenReturn(template);
        when(ig.getGroupName()).thenReturn("test-group");
        when(instanceMetaData.getInstanceGroup()).thenReturn(ig);
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("test");
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getId()).thenReturn(1L);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        doThrow(new CloudbreakOrchestratorFailedException("test")).when(hostOrchestrator).enableSeLinuxOnNodes(any(), any(), any());
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.modifySeLinuxOnAllNodes(stack));
        assertEquals("test", exception.getMessage());
    }
}
