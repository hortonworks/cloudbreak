package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.PROVIDER_NOT_RUNNING_VMS_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class VmStatusCheckerConclusionStepTest {

    private static final Long INSTANCE_DB_ID_1 = 1L;

    private static final Long INSTANCE_DB_ID_2 = 2L;

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private VmStatusCheckerConclusionStep underTest;

    private ClusterApi connector = mock(ClusterApi.class);

    private ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);

    @BeforeEach
    public void setUp() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
    }

    @Test
    public void checkShouldBeSuccessfulIfAllInstancesAreHealthy() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong()))
                .thenReturn(List.of(createInstanceMetadata(INSTANCE_DB_ID_1), createInstanceMetadata(INSTANCE_DB_ID_2)));
        when(stackInstanceStatusChecker.queryInstanceStatuses(any(), anyList()))
                .thenReturn(List.of(createCloudVmInstanceStatus(INSTANCE_DB_ID_1, true), createCloudVmInstanceStatus(INSTANCE_DB_ID_2, true)));

        Conclusion conclusion = underTest.check(STACK_ID);
        assertFalse(conclusion.isFailureFound());
        assertNull(conclusion.getConclusion());
        assertNull(conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfUnhealthyInstanceExists() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(PROVIDER_NOT_RUNNING_VMS_FOUND), any(Object[].class))).thenReturn("provider not running vms");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS), any(Object[].class)))
                .thenReturn("provider not running vms details");
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong()))
                .thenReturn(List.of(createInstanceMetadata(INSTANCE_DB_ID_1), createInstanceMetadata(INSTANCE_DB_ID_2)));
        when(stackInstanceStatusChecker.queryInstanceStatuses(any(), anyList()))
                .thenReturn(List.of(createCloudVmInstanceStatus(INSTANCE_DB_ID_1, false), createCloudVmInstanceStatus(INSTANCE_DB_ID_2, false)));

        Conclusion conclusion = underTest.check(STACK_ID);
        assertTrue(conclusion.isFailureFound());
        assertEquals("provider not running vms", conclusion.getConclusion());
        assertEquals("provider not running vms details", conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfUnknownInstanceExists() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(PROVIDER_NOT_RUNNING_VMS_FOUND), any(Object[].class))).thenReturn("provider not running vms");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(PROVIDER_NOT_RUNNING_VMS_FOUND_DETAILS), any(Object[].class)))
                .thenReturn("provider not running vms details");
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("unknown1");
        instanceMetaData.setId(INSTANCE_DB_ID_1);
        List<InstanceMetadataView> instanceMetaDataList = List.of(instanceMetaData);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong())).thenReturn(instanceMetaDataList);
        when(stackInstanceStatusChecker.queryInstanceStatuses(any(), anyList()))
                .thenReturn(List.of(createCloudVmInstanceStatus(INSTANCE_DB_ID_1, false)));

        Conclusion conclusion = underTest.check(INSTANCE_DB_ID_1);
        assertTrue(conclusion.isFailureFound());
        assertEquals("provider not running vms", conclusion.getConclusion());
        assertEquals("provider not running vms details", conclusion.getDetails());
        assertEquals(VmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    private ExtendedHostStatuses createExtendedHostStatuses(boolean healthy) {
        Map<HostName, Set<HealthCheck>> hostStatuses = new HashMap<>();
        HealthCheckResult status = healthy ? HealthCheckResult.HEALTHY : HealthCheckResult.UNHEALTHY;
        String reason = healthy ? null : "error";
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, status, Optional.ofNullable(reason), Optional.empty()));
        hostStatuses.put(HostName.hostName("host1"), healthChecks);
        hostStatuses.put(HostName.hostName("host2"), healthChecks);
        return new ExtendedHostStatuses(hostStatuses);
    }

    private InstanceMetadataView createInstanceMetadata(Long id) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("host" + id);
        instanceMetaData.setInstanceId("host" + id);
        instanceMetaData.setId(id);
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        return instanceMetaData;
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(Long id, boolean healthy) {
        com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus
                = healthy ? com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED : com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.FAILED;
        return new CloudVmInstanceStatus(new CloudInstance(String.valueOf("host" + id), null, null, null, null, Map.of(CloudInstance.ID, id)),
                instanceStatus);
    }
}
