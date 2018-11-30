package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class ClusterServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @InjectMocks
    private ClusterService clusterService;

    private Cluster cluster;

    private Stack stack;

    @Spy
    private StackUtil stackUtil;

    @Before
    public void setUp() throws TransactionExecutionException {
        cluster = new Cluster();
        cluster.setId(1L);
        cluster.setRdsConfigs(Set.of());
        stack = spy(new Stack());
        stack.setCluster(cluster);
        stack.setPlatformVariant("AWS");
        when(stackService.get(any(Long.class))).thenReturn(stack);

        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void repairClusterHostGroupsHappyPath() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        Constraint constraint = new Constraint();
        constraint.setInstanceGroup(instanceGroup);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setConstraint(constraint);

        HostMetadata host1Metadata = new HostMetadata();
        host1Metadata.setHostName("host1Name");
        host1Metadata.setHostGroup(hostGroup1);
        host1Metadata.setHostMetadataState(HostMetadataState.UNHEALTHY);

        HostMetadata host2Metadata = new HostMetadata();
        host2Metadata.setHostName("host2Name.healthy");
        host2Metadata.setHostGroup(hostGroup1);
        host2Metadata.setHostMetadataState(HostMetadataState.HEALTHY);

        Set<HostMetadata> hostMetadata = Set.of(host1Metadata, host2Metadata);
        hostGroup1.setHostMetadata(hostMetadata);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        clusterService.repairCluster(1L, List.of("hostGroup1"), false);

        verify(stack, never()).getInstanceMetaDataAsList();
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name"))), eq(false));
    }

    @Test
    public void repairClusterNodeIdsHappyPath() throws IOException {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        Constraint constraint = new Constraint();
        constraint.setInstanceGroup(instanceGroup);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setConstraint(constraint);

        HostMetadata host1Metadata = new HostMetadata();
        host1Metadata.setHostName("host1Name.healthy");
        host1Metadata.setHostGroup(hostGroup1);
        host1Metadata.setHostMetadataState(HostMetadataState.HEALTHY);

        Set<HostMetadata> hostMetadata = Set.of(host1Metadata);
        hostGroup1.setHostMetadata(hostMetadata);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));

        InstanceMetaData instance1md = new InstanceMetaData();
        instance1md.setInstanceId("instanceId1");
        instance1md.setDiscoveryFQDN("host1Name.healthy");

        List<InstanceMetaData> instanceMetaData = List.of(instance1md);

        when(stack.getInstanceMetaDataAsList()).thenReturn(instanceMetaData);


        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", 100, "standard",
                Boolean.TRUE, "", List.of());
        attributes.setDeleteOnTermination(null);
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("instanceId1");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);
        stack.setResources(Set.of(volumeSet));

        clusterService.repairCluster(1L, List.of("instanceId1"), false, false);
        verify(stack).getInstanceMetaDataAsList();
        verify(stack).getDiskResources();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceRepository).saveAll(saveCaptor.capture());
        assertFalse(stackUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name.healthy"))), eq(false));
    }
}