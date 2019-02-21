package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperErrorHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private InstanceGroupRepository instanceGroupRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ContainerOrchestrator orchestrator;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private ServiceProviderConnectorAdapter connector;

    @InjectMocks
    private ClusterBootstrapperErrorHandler underTest;

    @Test
    public void clusterBootstrapErrorHandlerWhenNodeCountLessThanOneAfterTheRollbackThenClusterProvisionFailed() throws CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<>());
        when(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer((Answer<InstanceMetaData>) invocation -> {
            Object[] args = invocation.getArguments();
            String ip = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getPrivateIp().equals(ip)) {
                    return instanceMetaData;
                }
            }
            return null;
        });
        when(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer((Answer<InstanceGroup>) invocation -> {
            Object[] args = invocation.getArguments();
            String name = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                    InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
                    instanceGroup.getInstanceMetaDataSet().forEach(im -> im.setInstanceStatus(InstanceStatus.TERMINATED));
                    return instanceGroup;
                }
            }
            return null;
        });
        when(cloudbreakMessagesService.getMessage(eq("bootstrapper.error.invalide.nodecount"), any())).thenReturn("invalide.nodecount");
        thrown.expect(CloudbreakOrchestratorFailedException.class);
        thrown.expectMessage("invalide.nodecount");

        underTest.terminateFailedNodes(null, orchestrator, TestUtil.stack(),
                new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 8443, false), prepareNodes(stack));
    }

    @Test
    public void clusterBootstrapErrorHandlerWhenNodeCountHigherThanZeroAfterTheRollbackThenClusterProvisionFailed()
            throws CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<>());
        when(instanceGroupRepository.save(any(InstanceGroup.class))).then(returnsFirstArg());
        when(instanceMetaDataRepository.save(any(InstanceMetaData.class))).then(returnsFirstArg());
        when(resourceRepository.findByStackIdAndNameAndType(nullable(Long.class), nullable(String.class), nullable(ResourceType.class)))
                .thenReturn(new Resource());
        when(connector.removeInstances(any(Stack.class), anySet(), anyString())).thenReturn(new HashSet<>());
        when(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer((Answer<InstanceMetaData>) invocation -> {
            Object[] args = invocation.getArguments();
            String ip = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getPrivateIp().equals(ip)) {
                    return instanceMetaData;
                }
            }
            return null;
        });
        when(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer((Answer<InstanceGroup>) invocation -> {
            Object[] args = invocation.getArguments();
            String name = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                    return instanceMetaData.getInstanceGroup();
                }
            }
            return null;
        });
        underTest.terminateFailedNodes(null, orchestrator, TestUtil.stack(),
                new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 8443, false), prepareNodes(stack));

        verify(eventService, times(4)).fireCloudbreakEvent(anyLong(), anyString(), nullable(String.class));
        verify(instanceGroupRepository, times(3)).save(any(InstanceGroup.class));
        verify(instanceMetaDataRepository, times(3)).save(any(InstanceMetaData.class));
        verify(connector, times(3)).removeInstances(any(Stack.class), anySet(), anyString());
        verify(resourceRepository, times(3)).findByStackIdAndNameAndType(anyLong(), anyString(), nullable(ResourceType.class));
        verify(resourceRepository, times(3)).delete(nullable(Resource.class));
        verify(instanceGroupRepository, times(3)).findOneByGroupNameInStack(anyLong(), anyString());

    }

    private Set<Node> prepareNodes(Stack stack) {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper()));
        }
        return nodes;
    }

}