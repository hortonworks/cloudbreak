package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
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
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperErrorHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ResourceService resourceService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private HostMetadataService hostMetadataService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ContainerOrchestrator orchestrator;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private ServiceProviderConnectorAdapter connector;

    @Mock
    private Clock clock;

    @InjectMocks
    private ClusterBootstrapperErrorHandler underTest;

    @Test
    public void clusterBootstrapErrorHandlerWhenNodeCountLessThanOneAfterTheRollbackThenClusterProvisionFailed() throws CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<>());
        when(instanceMetaDataService.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer((Answer<Optional<InstanceMetaData>>) invocation -> {
            Object[] args = invocation.getArguments();
            String ip = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getPrivateIp().equals(ip)) {
                    return Optional.of(instanceMetaData);
                }
            }
            return Optional.empty();
        });
        when(instanceGroupService.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer((Answer<Optional<InstanceGroup>>) invocation -> {
            Object[] args = invocation.getArguments();
            String name = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                    InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
                    instanceGroup.getInstanceMetaDataSet().forEach(im -> im.setInstanceStatus(InstanceStatus.TERMINATED));
                    return Optional.of(instanceGroup);
                }
            }
            return Optional.empty();
        });
        when(cloudbreakMessagesService.getMessage(eq("bootstrapper.error.invalide.nodecount"), any())).thenReturn("invalide.nodecount");
        thrown.expect(CloudbreakOrchestratorFailedException.class);
        thrown.expectMessage("invalide.nodecount");

        underTest.terminateFailedNodes(null, orchestrator, TestUtil.stack(),
                new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 443, "instanceId", false), prepareNodes(stack));
    }

    @Test
    public void clusterBootstrapErrorHandlerWhenNodeCountHigherThanZeroAfterTheRollbackThenClusterProvisionFailed()
            throws CloudbreakOrchestratorFailedException {
        Stack stack = TestUtil.stack();

        when(orchestrator.getAvailableNodes(any(GatewayConfig.class), anySet())).thenReturn(new ArrayList<>());
        when(instanceGroupService.save(any(InstanceGroup.class))).then(returnsFirstArg());
        when(instanceMetaDataService.save(any(InstanceMetaData.class))).then(returnsFirstArg());
        when(resourceService.findByStackIdAndNameAndType(nullable(Long.class), nullable(String.class), nullable(ResourceType.class)))
                .thenReturn(Optional.of(new Resource()));
        when(connector.removeInstances(any(Stack.class), anySet(), anyString())).thenReturn(new HashSet<>());
        when(instanceMetaDataService.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer((Answer<Optional<InstanceMetaData>>) invocation -> {
            Object[] args = invocation.getArguments();
            String ip = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getPrivateIp().equals(ip)) {
                    return Optional.of(instanceMetaData);
                }
            }
            return Optional.empty();
        });
        when(instanceGroupService.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer((Answer<Optional<InstanceGroup>>) invocation -> {
            Object[] args = invocation.getArguments();
            String name = (String) args[1];
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                if (instanceMetaData.getInstanceGroup().getGroupName().equals(name)) {
                    return Optional.ofNullable(instanceMetaData.getInstanceGroup());
                }
            }
            return Optional.empty();
        });
        underTest.terminateFailedNodes(null, orchestrator, TestUtil.stack(),
                new GatewayConfig("10.0.0.1", "198.0.0.1", "10.0.0.1", 443, "instanceId", false), prepareNodes(stack));

        verify(eventService, times(4)).fireCloudbreakEvent(anyLong(), anyString(), nullable(String.class));
        verify(instanceGroupService, times(3)).save(any(InstanceGroup.class));
        verify(instanceMetaDataService, times(3)).save(any(InstanceMetaData.class));
        verify(connector, times(3)).removeInstances(any(Stack.class), anySet(), anyString());
        verify(resourceService, times(3)).findByStackIdAndNameAndType(anyLong(), anyString(), nullable(ResourceType.class));
        verify(resourceService, times(3)).delete(nullable(Resource.class));
        verify(instanceGroupService, times(3)).findOneByGroupNameInStack(anyLong(), anyString());

    }

    private Set<Node> prepareNodes(Stack stack) {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper(), null, null));
        }
        return nodes;
    }

}