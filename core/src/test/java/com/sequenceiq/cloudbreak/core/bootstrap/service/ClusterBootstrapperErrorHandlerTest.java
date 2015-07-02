package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperErrorHandlerTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private InstanceGroupRepository instanceGroupRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @Mock
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Mock
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Mock
    private ContainerOrchestrator orchestrator;

    @Mock
    private CloudPlatformConnector cloudPlatformConnector;

    @Mock
    private MetadataSetup metadataSetup;

    @InjectMocks
    private ClusterBootstrapperErrorHandler underTest;

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void clusterBootstrapErrorHandlerWhenNodeCountLessThanOneAfterTheRollbackThenClusterProvisionFailed() throws CloudbreakOrchestratorFailedException {
        final Stack stack = TestUtil.stack();

        doNothing().when(eventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<String>());
        when(instanceGroupRepository.save(any(InstanceGroup.class))).then(returnsFirstArg());
        when(instanceMetaDataRepository.save(any(InstanceMetaData.class))).then(returnsFirstArg());
        when(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer(new Answer<InstanceMetaData>() {
            @Override
            public InstanceMetaData answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String ip = (String) args[1];
                for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                    if (instanceMetaData.getPrivateIp().equals(ip)) {
                        return instanceMetaData;
                    }
                }
                return null;
            }
        });
        when(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer(new Answer<InstanceGroup>() {
            @Override
            public InstanceGroup answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String name = (String) args[1];
                for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                    if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                        return instanceMetaData.getInstanceGroup();
                    }
                }
                return null;
            }
        });

        underTest.terminateFailedNodes(orchestrator, TestUtil.stack(), new GatewayConfig("10.0.0.1", "/cert/1"), prepareNodes(stack));
    }

    @Test
    public void clusterBootstrapErrorHandlerWhenNodeCountHigherThanZeroAfterTheRollbackThenClusterProvisionFailed()
            throws CloudbreakOrchestratorFailedException {
        final Stack stack = TestUtil.stack();

        doNothing().when(eventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<String>());
        when(instanceGroupRepository.save(any(InstanceGroup.class))).then(returnsFirstArg());
        when(instanceMetaDataRepository.save(any(InstanceMetaData.class))).then(returnsFirstArg());
        when(cloudPlatformConnectors.get(any(CloudPlatform.class))).thenReturn(cloudPlatformConnector);
        when(metadataSetups.get(any(CloudPlatform.class))).thenReturn(metadataSetup);
        when(metadataSetup.getInstanceResourceType()).thenReturn(ResourceType.AZURE_VIRTUAL_MACHINE);
        doNothing().when(resourceRepository).delete(anyLong());
        when(resourceRepository.findByStackIdAndNameAndType(anyLong(), anyString(), any(ResourceType.class))).thenReturn(new Resource());
        when(cloudPlatformConnector.removeInstances(any(Stack.class), anySet(), anyString())).thenReturn(new HashSet<String>());
        when(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer(new Answer<InstanceMetaData>() {
            @Override
            public InstanceMetaData answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String ip = (String) args[1];
                for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                    if (instanceMetaData.getPrivateIp().equals(ip)) {
                        return instanceMetaData;
                    }
                }
                return null;
            }
        });
        when(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer(new Answer<InstanceGroup>() {
            @Override
            public InstanceGroup answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                String name = (String) args[1];
                for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                    if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                        InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
                        instanceGroup.setNodeCount(2);
                        return instanceGroup;
                    }
                }
                return null;
            }
        });
        underTest.terminateFailedNodes(orchestrator, TestUtil.stack(), new GatewayConfig("10.0.0.1", "/cert/1"), prepareNodes(stack));

        verify(eventService, times(4)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        verify(instanceGroupRepository, times(3)).save(any(InstanceGroup.class));
        verify(instanceMetaDataRepository, times(3)).save(any(InstanceMetaData.class));
        verify(cloudPlatformConnectors, times(3)).get(any(CloudPlatform.class));
        verify(cloudPlatformConnector, times(3)).removeInstances(any(Stack.class), anySet(), anyString());
        verify(metadataSetup, times(3)).getInstanceResourceType();
        verify(resourceRepository, times(3)).findByStackIdAndNameAndType(anyLong(), anyString(), any(ResourceType.class));
        verify(metadataSetups, times(3)).get(any(CloudPlatform.class));
        verify(resourceRepository, times(3)).delete(anyLong());
        verify(instanceGroupRepository, times(3)).findOneByGroupNameInStack(anyLong(), anyString());

    }

    private Set<Node> prepareNodes(Stack stack) {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp()));
        }
        return nodes;
    }

}