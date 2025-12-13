package com.sequenceiq.cloudbreak.conclusion.step;

import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_SERVER_UNREACHABLE;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND;
import static com.sequenceiq.cloudbreak.conclusion.step.ConclusionMessage.CM_UNHEALTHY_VMS_FOUND_DETAILS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class CmStatusCheckerConclusionStepTest {

    @Mock
    private StackService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private CmStatusCheckerConclusionStep underTest;

    private ClusterApi connector = mock(ClusterApi.class);

    private ClusterStatusService clusterStatusService = mock(ClusterStatusService.class);

    @BeforeEach
    public void setUp() {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        when(stackService.getById(anyLong())).thenReturn(stack);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        lenient().when(runtimeVersionService.getRuntimeVersion(anyLong())).thenReturn(Optional.of("7.2.11"));
    }

    @Test
    public void checkShouldBeSuccessfulIfAllInstancesAreHealthy() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong()))
                .thenReturn(List.of(createInstanceMetadata("host1"), createInstanceMetadata("host2")));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(createExtendedHostStatuses(true));

        Conclusion conclusion = underTest.check(1L);
        assertFalse(conclusion.isFailureFound());
        assertNull(conclusion.getConclusion());
        assertNull(conclusion.getDetails());
        assertEquals(CmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfUnhealthyOrUnknownInstanceExists() {
        when(cloudbreakMessagesService.getMessageWithArgs(eq(CM_UNHEALTHY_VMS_FOUND), any(Object[].class))).thenReturn("cm unhealthy vms");
        when(cloudbreakMessagesService.getMessageWithArgs(eq(CM_UNHEALTHY_VMS_FOUND_DETAILS), any(Object[].class))).thenReturn("cm unhealthy vms details");
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.TRUE);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong()))
                .thenReturn(List.of(createInstanceMetadata("host1"), createInstanceMetadata("host2"), createInstanceMetadata("host3")));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(createExtendedHostStatuses(false));

        Conclusion conclusion = underTest.check(1L);
        assertTrue(conclusion.isFailureFound());
        assertEquals("cm unhealthy vms", conclusion.getConclusion());
        assertEquals("cm unhealthy vms details", conclusion.getDetails());
        assertEquals(CmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldFailIfCMIsNotRunning() {
        when(cloudbreakMessagesService.getMessage(eq(CM_SERVER_UNREACHABLE))).thenReturn("cm server unreachable");
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong()))
                .thenReturn(List.of(createInstanceMetadata("host1"), createInstanceMetadata("host2")));

        Conclusion conclusion = underTest.check(1L);
        assertTrue(conclusion.isFailureFound());
        assertEquals("cm server unreachable", conclusion.getConclusion());
        assertEquals("cm server unreachable", conclusion.getDetails());
        assertEquals(CmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
    }

    @Test
    public void checkShouldHandleMissingFqdns() {
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(Boolean.FALSE);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("1");
        List<InstanceMetadataView> instanceMetaDataList = List.of(instanceMetaData);
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong())).thenReturn(instanceMetaDataList);

        Conclusion conclusion = underTest.check(1L);
        assertTrue(conclusion.isFailureFound());
        assertNull(conclusion.getConclusion());
        assertNull(conclusion.getDetails());
        assertEquals(CmStatusCheckerConclusionStep.class, conclusion.getConclusionStepClass());
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

    private InstanceMetadataView createInstanceMetadata(String host) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(host);
        instanceMetaData.setInstanceId(host);
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        return instanceMetaData;
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(String instanceId, boolean healthy) {
        com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus
                = healthy ? com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED : com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.FAILED;
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null, null, null),
                instanceStatus);
    }
}